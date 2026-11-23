# OpsPilot - Event Contracts Specification

## 1. Overview & Topic Mapping

OpsPilot uses Apache Kafka with KRaft mode as its asynchronous messaging backbone. All events published to Kafka must conform to standard versioned JSON schemas defined in `contracts/events/` and wrap their domain payloads in the standard distributed envelope (`contracts/schemas/common-envelope-v1.json`).

| Kafka Topic | Producer | Consumer | Schema Contract | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| `incident.alerts.v1` | External Webhook / Mock Producer | Spring Boot Control Plane | `incident-alert-v1.json` | High-throughput ingested operational alerts |
| `incident.triage.requested.v1` | Spring Boot (Outbox Publisher) | Python AI Triage Engine | `incident-triage-requested-v1.json` | Requests AI investigation for a new or updated incident |
| `incident.triage.completed.v1` | Python AI Triage Engine | Spring Boot Control Plane | `incident-triage-completed-v1.json` | Structured investigation findings, evidence, citations, & remediations |
| `incident.triage.dlq.v1` | Python AI / Spring Boot | Dead Letter Queue Monitor | `incident-triage-dlq-v1.json` | Failed, unparseable, or repeatedly retried poison messages |

---

## 2. Common Distributed Envelope (`common-envelope-v1.json`)

Every message published to Kafka must use this outer envelope structure:

```json
{
  "eventId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "eventType": "incident.triage.requested.v1",
  "schemaVersion": "1.0.0",
  "tenantId": "org-corp-enterprise-01",
  "timestamp": "2026-11-23T18:50:00Z",
  "correlationId": "corr-987654321-abcdef",
  "traceContext": {
    "traceParent": "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
    "traceState": ""
  },
  "payload": { ... }
}
```

### Key Envelope Attributes:
- **`eventId`** (UUID v4): Unique identifier used for consumer deduplication and idempotency tables.
- **`tenantId`** (String): Enforces tenant boundary routing and prevents cross-tenant contamination.
- **`correlationId`** (String): Preserved across all asynchronous boundaries for end-to-end log aggregation.
- **`traceContext`** (Object): W3C Trace Context headers propagated via Kafka message headers and envelope payload for distributed OpenTelemetry spans.

---

## 3. Topic Message Specifications

### 3.1 `incident.triage.requested.v1`
Published when an incident is opened or updated and requires automated AI triage:

```json
{
  "incidentId": "8f3b60aa-1f5e-4c7b-a25e-29f95d5cb211",
  "title": "High HTTP 500 error rate on checkout-service",
  "severity": "CRITICAL",
  "serviceName": "checkout-service",
  "environment": "production",
  "status": "OPEN",
  "alertCount": 3,
  "initialAlertSummary": "HTTP 500 spike exceeding 15% threshold over 5m window",
  "fingerprints": ["chkout-http500-prod-c6b7"],
  "recentAlerts": [
    {
      "alertId": "alert-prom-9981",
      "source": "Prometheus",
      "timestamp": "2026-11-23T18:48:10Z",
      "message": "HTTP 5xx rate > 15% on checkout-service-pod-x78"
    }
  ]
}
```

### 3.2 `incident.triage.completed.v1`
Emitted by the Python AI Triage engine upon completing graph execution:

```json
{
  "incidentId": "8f3b60aa-1f5e-4c7b-a25e-29f95d5cb211",
  "triageId": "9b1c7a88-43d2-4e01-9f93-559d9c228fa4",
  "investigationDurationMs": 1420,
  "confidenceScore": 0.88,
  "observations": [
    "checkout-service returned 18.4% HTTP 500 responses between 18:45 and 18:50 UTC.",
    "Database connection pool saturation reached 100% on postgres-orders-replica.",
    "Deployment commit `f89a2b1` was rolled out 12 minutes prior to the alert spike."
  ],
  "supportingEvidence": [
    {
      "source": "Prometheus",
      "evidenceType": "METRIC",
      "description": "hikari_pool_active_connections reached max limit (50/50)",
      "timestamp": "2026-11-23T18:47:00Z",
      "metricValue": 50,
      "threshold": 45
    },
    {
      "source": "GitLab CI",
      "evidenceType": "DEPLOYMENT",
      "description": "Release v2.4.1 deployed commit f89a2b1 with missing connection timeout config",
      "timestamp": "2026-11-23T18:36:00Z"
    }
  ],
  "potentialRootCauses": [
    {
      "hypothesis": "Unbounded database queries introduced in v2.4.1 causing connection pool exhaustion.",
      "likelihood": "HIGH",
      "reasoning": "Connection pool usage spiked from 20% to 100% immediately following deployment f89a2b1."
    }
  ],
  "suggestedRemediations": [
    {
      "actionTitle": "Rollback checkout-service to v2.4.0",
      "actionDescription": "Revert deployment to commit e17b98d to restore stable database pool configurations.",
      "riskLevel": "MEDIUM",
      "requiresApproval": true
    }
  ],
  "runbookCitations": [
    {
      "documentId": "rb-db-pool-exhaustion",
      "title": "Database Connection Pool Saturation Runbook",
      "chunkId": "chunk-03",
      "section": "Remediation - Connection Pool Leak",
      "snippet": "If pool saturation persists > 5 minutes after release, trigger canary rollback...",
      "relevanceScore": 0.94
    }
  ],
  "uncertainties": [],
  "additionalInvestigationSteps": [
    "Inspect PostgreSQL slow query logs for checkout-service queries taking > 500ms."
  ],
  "completedAt": "2026-11-23T18:50:35Z"
}
```

---

## 4. Idempotency & Consumer Error Handling

1. **Idempotency Key**: Consumers check `processed_events` table using `(tenant_id, event_id)` unique composite constraint. Duplicate events are acknowledged without re-execution.
2. **Retry Strategy**: Transient failures retry up to 3 times with exponential backoff (`1s`, `2s`, `4s`).
3. **Dead Letter Queue Routing**: Non-retryable errors (schema validation failures, corrupted payloads) or exhausted retries are published to `incident.triage.dlq.v1` with root cause stack trace.
