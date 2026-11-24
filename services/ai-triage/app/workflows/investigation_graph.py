"""
Stateful multi-node investigation workflow orchestrated with LangGraph.

Graph Topology:
  [collect_evidence]
          │
          ▼
  [retrieve_runbooks]
          │
          ▼
  [generate_hypotheses]
          │
          ▼
  [verify_guardrails]
          │
          ▼
        [END]
"""

import logging
import time
import uuid
from typing import Dict, Any, List, Optional
from pydantic import BaseModel, Field

from langgraph.graph import StateGraph, START, END

from app.services.hybrid_retriever import HybridRetriever
from app.services.mcp_client import McpClient

logger = logging.getLogger(__name__)


class InvestigationState(BaseModel):
    # Inputs
    incident_id: str
    tenant_id: str
    service_name: str
    severity: str
    environment: str
    title: str
    initial_summary: str
    fingerprints: List[str] = Field(default_factory=list)

    # Node Intermediate Outputs
    evidence: List[Dict[str, Any]] = Field(default_factory=list)
    observations: List[str] = Field(default_factory=list)
    runbook_citations: List[Dict[str, Any]] = Field(default_factory=list)
    potential_root_causes: List[Dict[str, Any]] = Field(default_factory=list)
    suggested_remediations: List[Dict[str, Any]] = Field(default_factory=list)
    uncertainties: List[str] = Field(default_factory=list)
    additional_steps: List[str] = Field(default_factory=list)

    # Final Guarded Output
    confidence_score: float = 0.85
    duration_ms: int = 0
    triage_id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    guardrails_passed: bool = False


# Node 1: Evidence Collector
def collect_evidence_node(state: InvestigationState) -> Dict[str, Any]:
    logger.info(f"[Node: collect_evidence] Gathering MCP operational telemetry for {state.service_name}")
    mcp = McpClient()

    evidence = list(state.evidence)
    observations = list(state.observations)

    # 1. Base telemetry from alert
    evidence.append({
        "source": "alert_manager",
        "evidenceType": "LOG",
        "description": f"Incoming Alert: {state.initial_summary}",
        "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
    })
    observations.append(f"Primary alert reported for {state.service_name}: {state.initial_summary}")

    # 2. Live MCP health checks
    health_data = mcp.get_service_health(state.service_name, state.environment)
    evidence.append({
        "source": "mcp-service-health",
        "evidenceType": "HEALTH_CHECK",
        "description": f"Service status: {health_data.get('status')}, CPU: {health_data.get('cpuUsagePercent')}%, ErrorRate: {health_data.get('errorRatePercent')}%",
        "metricValue": health_data.get("cpuUsagePercent"),
        "threshold": 80.0,
    })
    observations.append(
        f"MCP health probe confirms {state.service_name} status '{health_data.get('status')}' "
        f"with error rate at {health_data.get('errorRatePercent')}%"
    )

    # 3. Recent deployments from MCP
    deployments = mcp.get_recent_deployments(state.service_name, state.environment)
    if deployments:
        dep = deployments[0]
        evidence.append({
            "source": "mcp-deployment-registry",
            "evidenceType": "DEPLOYMENT",
            "description": f"Recent release {dep.get('version')} deployed at {dep.get('deployedAt')} by {dep.get('deployedBy')}",
        })
        observations.append(f"Correlating with deployment {dep.get('version')} released recently by {dep.get('deployedBy')}")

    return {"evidence": evidence, "observations": observations}


# Node 2: Runbook Retriever (Hybrid RAG)
def retrieve_runbooks_node(state: InvestigationState) -> Dict[str, Any]:
    logger.info(f"[Node: retrieve_runbooks] Running Hybrid RAG retrieval for query '{state.service_name} {state.initial_summary}'")
    retriever = HybridRetriever(rrf_k=60)
    query = f"{state.service_name} {state.initial_summary} {state.title}"

    citations = retriever.search(query, top_k=2)
    return {"runbook_citations": citations}


# Node 3: Hypothesis Generator
def generate_hypotheses_node(state: InvestigationState) -> Dict[str, Any]:
    logger.info(f"[Node: generate_hypotheses] Formulating potential root causes for incident {state.incident_id}")

    root_causes = []
    remediations = []
    uncertainties = []
    additional_steps = []

    # Correlate findings
    has_high_cpu = any("cpu" in obs.lower() or "latency" in obs.lower() for obs in state.observations)
    has_db_issue = any("pool" in obs.lower() or "connection" in obs.lower() or "database" in obs.lower() for obs in state.observations)

    if has_db_issue:
        root_causes.append({
            "hypothesis": f"Database connection pool starvation on {state.service_name}",
            "likelihood": "HIGH",
            "reasoning": "Telemetry and error signatures align with connection timeout exhaustion.",
        })
        remediations.append({
            "actionTitle": "Terminate blocking database queries",
            "actionDescription": "Run pg_terminate_backend on idle/blocking transaction PIDs.",
            "riskLevel": "MEDIUM",
            "requiresApproval": True,
        })
    elif has_high_cpu or state.severity in ("CRITICAL", "HIGH"):
        root_causes.append({
            "hypothesis": f"Resource saturation or regression from release on {state.service_name}",
            "likelihood": "HIGH",
            "reasoning": "Correlates directly with telemetry spikes reported in recent alerts and deployments.",
        })
        remediations.append({
            "actionTitle": f"Scale {state.service_name} pod replicas",
            "actionDescription": "Increase horizontal pod replicas by 50% to absorb load spike.",
            "riskLevel": "LOW",
            "requiresApproval": True,
        })
        remediations.append({
            "actionTitle": f"Roll back {state.service_name} deployment",
            "actionDescription": "Revert release to previous verified deployment commit.",
            "riskLevel": "HIGH",
            "requiresApproval": True,
        })
    else:
        root_causes.append({
            "hypothesis": f"Transient network jitter or downstream dependency latency on {state.service_name}",
            "likelihood": "MEDIUM",
            "reasoning": "Moderate error rate without total container failure.",
        })
        remediations.append({
            "actionTitle": f"Verify downstream upstream egress firewalls",
            "actionDescription": "Check latency to external payment gateways and third-party APIs.",
            "riskLevel": "LOW",
            "requiresApproval": True,
        })

    uncertainties.append("External upstream vendor SLA status not confirmed via telemetry")
    additional_steps.append("Check OpenTelemetry distributed trace waterfall for 5xx span details")
    additional_steps.append("Inspect host-level network packet drop metrics")

    confidence = 0.88 if state.severity == "CRITICAL" else 0.82

    return {
        "potential_root_causes": root_causes,
        "suggested_remediations": remediations,
        "uncertainties": uncertainties,
        "additional_steps": additional_steps,
        "confidence_score": confidence,
    }


# Node 4: Guardrail Verifier
def verify_guardrails_node(state: InvestigationState) -> Dict[str, Any]:
    logger.info(f"[Node: verify_guardrails] Validating safety invariants for incident {state.incident_id}")

    # Enterprise Safety Invariant 1:
    # ALL remediations MUST require human approval. Auto-execution is strictly forbidden.
    sanitized_remediations = []
    for r in state.suggested_remediations:
        sanitized = dict(r)
        sanitized["requiresApproval"] = True  # Strict enforcement
        sanitized_remediations.append(sanitized)

    # Invariant 2: Confidence score bounded in [0.0, 1.0]
    bounded_confidence = max(0.0, min(state.confidence_score, 1.0))

    return {
        "suggested_remediations": sanitized_remediations,
        "confidence_score": bounded_confidence,
        "guardrails_passed": True,
    }


# Compile StateGraph
def build_investigation_graph():
    builder = StateGraph(InvestigationState)

    builder.add_node("collect_evidence", collect_evidence_node)
    builder.add_node("retrieve_runbooks", retrieve_runbooks_node)
    builder.add_node("generate_hypotheses", generate_hypotheses_node)
    builder.add_node("verify_guardrails", verify_guardrails_node)

    builder.add_edge(START, "collect_evidence")
    builder.add_edge("collect_evidence", "retrieve_runbooks")
    builder.add_edge("retrieve_runbooks", "generate_hypotheses")
    builder.add_edge("generate_hypotheses", "verify_guardrails")
    builder.add_edge("verify_guardrails", END)

    return builder.compile()


investigation_graph = build_investigation_graph()
