"""
Integration and unit tests for Python AI Triage event processing,
contract compliance, DLQ routing, and mock triage generation.
"""

import json
import uuid
import pytest
from app.services.triage_pipeline import TriagePipelineService


@pytest.fixture
def triage_service():
    return TriagePipelineService()


def test_triage_pipeline_generates_valid_completed_event(triage_service):
    incident_id = str(uuid.uuid4())
    tenant_id = str(uuid.uuid4())

    incoming_event = {
        "eventId": f"req-{uuid.uuid4().hex[:8]}",
        "eventType": "incident.triage.requested.v1",
        "schemaVersion": "1.0.0",
        "tenantId": tenant_id,
        "timestamp": "2026-11-24T06:00:00Z",
        "correlationId": "corr-test-pipeline-1",
        "payload": {
            "incidentId": incident_id,
            "title": "[CRITICAL] High error rate on auth-service (production)",
            "severity": "CRITICAL",
            "serviceName": "auth-service",
            "environment": "production",
            "status": "OPEN",
            "alertCount": 3,
            "initialAlertSummary": "HTTP 500 error rate exceeded 15% over 5m",
            "fingerprints": ["fp-test-1", "fp-test-2"],
        },
    }

    result = triage_service.process_triage_request(json.dumps(incoming_event))

    assert result["eventType"] == "incident.triage.completed.v1"
    assert result["tenantId"] == tenant_id
    assert result["correlationId"] == "corr-test-pipeline-1"

    payload = result["payload"]
    assert payload["incidentId"] == incident_id
    assert payload["confidenceScore"] > 0.0
    assert payload["investigationDurationMs"] > 0
    assert len(payload["observations"]) >= 1
    assert len(payload["supportingEvidence"]) >= 1
    assert len(payload["potentialRootCauses"]) >= 1
    assert len(payload["suggestedRemediations"]) >= 1
    assert len(payload["runbookCitations"]) >= 1

    # Verify that all suggested remediations require human sign-off
    for rem in payload["suggestedRemediations"]:
        assert rem["requiresApproval"] is True


def test_triage_pipeline_routes_malformed_json_to_dlq(triage_service):
    malformed_json = "{bad-json: definitely-not-valid}"

    dlq_event = triage_service.process_triage_request(malformed_json)

    assert "originalEventId" in dlq_event
    assert dlq_event["originalTopic"] == "incident.triage.requested.v1"
    assert "JSONDecodeError" in dlq_event["exceptionClass"]
    assert dlq_event["retryCount"] >= 1
    assert dlq_event["rawPayload"] == malformed_json


def test_triage_pipeline_routes_missing_required_fields_to_dlq(triage_service):
    # Missing incidentId and tenantId
    invalid_event = {
        "eventId": "evt-incomplete",
        "eventType": "incident.triage.requested.v1",
        "payload": {
            "serviceName": "order-service"
        }
    }

    dlq_event = triage_service.process_triage_request(json.dumps(invalid_event))

    assert dlq_event["originalEventId"] == "evt-incomplete"
    assert "ValueError" in dlq_event["exceptionClass"]
    assert "incidentId" in dlq_event["failureReason"]
