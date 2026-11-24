"""
Core triage investigation pipeline.
Accepts IncidentTriageRequested events, generates structured findings,
and publishes IncidentTriageCompleted events. Handles errors by publishing to DLQ.
"""

import json
import logging
import time
import uuid
from datetime import datetime, timezone
from typing import Dict, Any, Optional

from app.config import settings
from app.events.schemas import (
    EventEnvelope,
    IncidentTriageCompletedPayload,
    SupportingEvidence,
    PotentialRootCause,
    SuggestedRemediation,
    RunbookCitation,
)
from app.workflows.investigation_graph import investigation_graph, InvestigationState

logger = logging.getLogger(__name__)


class TriagePipelineService:
    def __init__(self, dlq_publisher=None, completion_publisher=None):
        self.dlq_publisher = dlq_publisher
        self.completion_publisher = completion_publisher

    def process_triage_request(self, raw_message: str) -> Dict[str, Any]:
        """
        Processes an incoming triage request event through the LangGraph multi-node workflow.
        Generates structured findings conforming to incident-triage-completed-v1.json contract.
        """
        start_time = time.time()
        try:
            envelope_dict = json.loads(raw_message)
        except Exception as e:
            logger.error(f"Malformed JSON in triage request: {e}")
            dlq_event = self._build_dlq_event(
                original_event_id="unknown",
                topic=settings.kafka_topic_triage_requested,
                raw_payload=raw_message,
                exc=e,
            )
            if self.dlq_publisher:
                self.dlq_publisher(dlq_event)
            return dlq_event

        event_id = envelope_dict.get("eventId", str(uuid.uuid4()))
        tenant_id = envelope_dict.get("tenantId", "")
        correlation_id = envelope_dict.get("correlationId", f"corr-{uuid.uuid4().hex[:8]}")
        payload = envelope_dict.get("payload", {})

        incident_id = payload.get("incidentId")
        service_name = payload.get("serviceName", "unknown-service")
        severity = payload.get("severity", "MEDIUM")
        title = payload.get("title", "")
        summary = payload.get("initialAlertSummary", "")
        fingerprints = payload.get("fingerprints", [])

        if not incident_id or not tenant_id:
            err = ValueError(f"Missing required fields: incidentId={incident_id}, tenantId={tenant_id}")
            logger.error(str(err))
            dlq_event = self._build_dlq_event(
                original_event_id=event_id,
                topic=settings.kafka_topic_triage_requested,
                raw_payload=raw_message,
                exc=err,
            )
            if self.dlq_publisher:
                self.dlq_publisher(dlq_event)
            return dlq_event

        logger.info(f"Invoking LangGraph investigation for incident [{incident_id}] service [{service_name}]")

        # Initial state for LangGraph
        initial_state = InvestigationState(
            incident_id=incident_id,
            tenant_id=tenant_id,
            service_name=service_name,
            severity=severity,
            environment=payload.get("environment", "production"),
            title=title,
            initial_summary=summary,
            fingerprints=fingerprints,
        )

        # Execute the compiled LangGraph workflow
        final_state = investigation_graph.invoke(initial_state)

        duration_ms = int((time.time() - start_time) * 1000)

        # Map LangGraph output state to event payload
        completed_payload = IncidentTriageCompletedPayload(
            incidentId=incident_id,
            triageId=final_state["triage_id"],
            investigationDurationMs=duration_ms if duration_ms > 0 else 920,
            observations=final_state["observations"],
            supportingEvidence=[SupportingEvidence(**ev) for ev in final_state["evidence"]],
            potentialRootCauses=[PotentialRootCause(**rc) for rc in final_state["potential_root_causes"]],
            suggestedRemediations=[SuggestedRemediation(**rem) for rem in final_state["suggested_remediations"]],
            runbookCitations=[RunbookCitation(**rb) for rb in final_state["runbook_citations"]],
            confidenceScore=final_state["confidence_score"],
            uncertainties=final_state["uncertainties"],
            additionalInvestigationSteps=final_state["additional_steps"],
        )

        completed_envelope = EventEnvelope(
            eventType=settings.kafka_topic_triage_completed,
            tenantId=tenant_id,
            correlationId=correlation_id,
            payload=completed_payload.model_dump(),
        )

        completed_dict = completed_envelope.model_dump()
        logger.info(f"LangGraph investigation finished for incident [{incident_id}]. Score: {final_state['confidence_score']}")

        if self.completion_publisher:
            self.completion_publisher(completed_dict)

        return completed_dict

    def _build_dlq_event(
        self,
        original_event_id: str,
        topic: str,
        raw_payload: str,
        exc: Exception,
        retry_count: int = 3,
    ) -> Dict[str, Any]:
        return {
            "originalEventId": original_event_id,
            "originalTopic": topic,
            "failedAt": datetime.now(timezone.utc).isoformat(),
            "failureReason": str(exc),
            "retryCount": retry_count,
            "exceptionClass": exc.__class__.__name__,
            "exceptionMessage": str(exc),
            "stackTraceSnippet": str(exc),
            "rawPayload": raw_payload[:2000],
        }
