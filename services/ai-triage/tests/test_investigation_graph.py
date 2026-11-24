"""
Tests for Hybrid RAG retriever (dense vector + lexical FTS + RRF)
and LangGraph stateful multi-node investigation workflow.
"""

import pytest
from app.services.hybrid_retriever import HybridRetriever, tokenize
from app.workflows.investigation_graph import (
    investigation_graph,
    InvestigationState,
    collect_evidence_node,
    retrieve_runbooks_node,
    generate_hypotheses_node,
    verify_guardrails_node,
)


def test_tokenizer():
    tokens = tokenize("HikariPool-1 connection timeout in PostgreSQL 16!")
    assert "hikaripool" in tokens
    assert "1" in tokens
    assert "postgresql" in tokens
    assert "timeout" in tokens


def test_hybrid_retriever_dense_and_lexical_rrf_scoring():
    retriever = HybridRetriever(rrf_k=60)

    # Test 1: Query relating to database connection pool
    results = retriever.search("database connection pool exhausted timeout Hikari", top_k=2)
    assert len(results) >= 1
    top_doc = results[0]
    assert "Database Connection Pool" in top_doc["title"]
    assert top_doc["relevanceScore"] >= 0.70

    # Test 2: Query relating to CPU / latency spikes
    cpu_results = retriever.search("high CPU load sustained latency 504 gateway timeout", top_k=2)
    assert len(cpu_results) >= 1
    assert "High CPU and Latency" in cpu_results[0]["title"]


def test_langgraph_individual_nodes():
    state = InvestigationState(
        incident_id="inc-test-nodes-1",
        tenant_id="tenant-123",
        service_name="payment-service",
        severity="HIGH",
        environment="production",
        title="Payment Service Latency Spike",
        initial_summary="504 Gateway Timeout on checkout endpoints",
    )

    # 1. Collect evidence
    ev_out = collect_evidence_node(state)
    assert len(ev_out["evidence"]) >= 2
    assert len(ev_out["observations"]) >= 2
    state.evidence = ev_out["evidence"]
    state.observations = ev_out["observations"]

    # 2. Retrieve runbooks
    rb_out = retrieve_runbooks_node(state)
    assert len(rb_out["runbook_citations"]) >= 1
    state.runbook_citations = rb_out["runbook_citations"]

    # 3. Generate hypotheses
    hyp_out = generate_hypotheses_node(state)
    assert len(hyp_out["potential_root_causes"]) >= 1
    assert len(hyp_out["suggested_remediations"]) >= 1
    state.potential_root_causes = hyp_out["potential_root_causes"]
    state.suggested_remediations = hyp_out["suggested_remediations"]

    # 4. Verify guardrails
    g_out = verify_guardrails_node(state)
    assert g_out["guardrails_passed"] is True
    for r in g_out["suggested_remediations"]:
        assert r["requiresApproval"] is True


def test_langgraph_full_graph_invocation():
    initial_state = InvestigationState(
        incident_id="inc-graph-e2e",
        tenant_id="tenant-e2e",
        service_name="order-service",
        severity="CRITICAL",
        environment="production",
        title="Critical CrashLoopBackOff",
        initial_summary="OOMKilled exit code 137 on order worker pods",
    )

    final_state = investigation_graph.invoke(initial_state)

    assert final_state["guardrails_passed"] is True
    assert final_state["confidence_score"] >= 0.80
    assert len(final_state["observations"]) >= 2
    assert len(final_state["evidence"]) >= 2
    assert len(final_state["runbook_citations"]) >= 1
    assert len(final_state["potential_root_causes"]) >= 1

    # Verify enterprise safety requirement
    for rem in final_state["suggested_remediations"]:
        assert rem["requiresApproval"] is True
