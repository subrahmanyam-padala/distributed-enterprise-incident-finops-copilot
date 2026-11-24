# OpsPilot - Build State & Progress Ledger

## Current Status Summary

- **Current Milestone**: Milestone 3 — Event-Driven Architecture & Kafka Pipeline
- **Status**: Completed (Tests passed: Java 38/38, Python AI 5/5, Python MCP 4/4, Web 2/2)
- **Next Milestone**: Milestone 4 — LangGraph AI Investigation & Hybrid RAG
- **Last Verified Date**: 2026-11-24

---

## 1. Development Environment Audit

| Tool | Detected Version | Target Specification | Status | Notes |
| :--- | :--- | :--- | :---: | :--- |
| **Operating System** | Windows (PowerShell) | Windows / Linux / macOS | OK | Native workspace |
| **Java JDK** | OpenJDK 21.0.10 LTS (Temurin) | Java 21 LTS | **PASS** | Verified with Spring Boot 3.4.0 |
| **Maven** | Apache Maven 3.9.9 | Maven 3.9+ / `mvnw` wrapper | **PASS** | Installed and verified via `mvnw.cmd` |
| **Python** | 3.13.0 | Python 3.11+ | **PASS** | FastAPI, pytest, aiokafka verified |
| **Node.js** | v22.20.0 | Node 20+ LTS | **PASS** | Vite, React 18, Vitest verified |
| **npm** | 10.9.3 | npm 10+ | **PASS** | 408 packages installed cleanly |
| **Docker Engine** | 29.7.2 | Docker Engine 24+ | **PASS** | Docker Compose configured |
| **Git** | 2.47.1.windows.2 | Git 2.40+ | **PASS** | Configured: `subrahmanyam-padala` / `subbhu35@gmail.com` |

---

## 2. Implemented Functionality (Milestone 3)

1. **Transactional Outbox Publisher (`ScheduledOutboxPublisher`)**:
   - Poller periodically reading `PENDING` events from `outbox_events` table with configurable batch size and interval.
   - Synchronous send to Apache Kafka broker ensuring at-least-once delivery semantics.
   - Updates status to `PUBLISHED` on broker acknowledgment or increments `retry_count` and marks `FAILED` on max retries.
2. **Kafka Event Producer (`KafkaEventProducer`)**:
   - Spring Kafka `KafkaTemplate` wrapper with asynchronous completion callbacks, partition logging, and structured error reporting.
3. **Python AI Triage Kafka Pipeline & Consumer Worker**:
   - `aiokafka` consumer listening to `incident.triage.requested.v1`.
   - `TriagePipelineService` generating structured investigation findings strictly matching `incident-triage-completed-v1.json`.
   - Automatic Dead Letter Queue (`incident.triage.dlq.v1`) publishing for malformed payloads or missing required fields.
   - Guarded remediations: all generated operational recommendations explicitly enforce `requiresApproval: true`.
4. **Idempotent Result Consumer in Java Control Plane (`TriageCompletedConsumer`)**:
   - Listens to `incident.triage.completed.v1`.
   - Checks `processed_events` table using composite key `(event_id, organization_id)` before persisting to prevent duplicate processing.
   - Persists structured investigation reports to `triage_reports` table.
   - Automatically moves incident status from `OPEN` to `INVESTIGATING`.
   - Records immutable tenant audit trail entry for each persisted investigation.
5. **API & Incident Detail Enrichment**:
   - `GET /api/v1/incidents/{id}/triage` endpoint delivering tenant-isolated investigation reports.

---

## 3. Files Created or Modified (Milestone 3)

- `services/control-plane/src/main/java/com/opspilot/controlplane/kafka/KafkaEventProducer.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/kafka/ScheduledOutboxPublisher.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/kafka/TriageCompletedConsumer.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/domain/entity/TriageReport.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/domain/entity/ProcessedEvent.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/repository/TriageReportRepository.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/repository/ProcessedEventRepository.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/dto/triage/TriageReportResponse.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/service/IncidentService.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/controller/IncidentController.java`
- `services/control-plane/src/test/java/com/opspilot/controlplane/kafka/KafkaPipelineIntegrationTest.java`
- `services/control-plane/src/test/resources/application-test.yml`
- `services/ai-triage/app/events/schemas.py`
- `services/ai-triage/app/events/consumer.py`
- `services/ai-triage/app/services/triage_pipeline.py`
- `services/ai-triage/app/main.py`
- `services/ai-triage/tests/test_triage_pipeline.py`
- `docs/BUILD_STATE.md`

---

## 4. Verification & Test Execution Results

| Test Suite | Framework | Total Tests | Passed | Failed | Status |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **Java Control Plane** | JUnit 5 / Spring Boot Test | 38 | 38 | 0 | **PASS** |
| - *Health Check* | MockMvc | 1 | 1 | 0 | **PASS** |
| - *Auth Controller* | MockMvc | 9 | 9 | 0 | **PASS** |
| - *Alert Webhook* | MockMvc | 8 | 8 | 0 | **PASS** |
| - *Incident Lifecycle* | MockMvc | 12 | 12 | 0 | **PASS** |
| - *Tenant Isolation* | MockMvc | 6 | 6 | 0 | **PASS** |
| - *Kafka Pipeline & Idempotency* | MockMvc / Spring MockBean | 2 | 2 | 0 | **PASS** |
| **Python AI Triage Engine** | Pytest | 5 | 5 | 0 | **PASS** |
| - *Health & Liveness* | TestClient | 2 | 2 | 0 | **PASS** |
| - *Triage Pipeline & DLQ* | Pytest unit | 3 | 3 | 0 | **PASS** |
| **Python MCP Server** | Pytest / TestClient | 4 | 4 | 0 | **PASS** |
| **React Web Frontend** | Vitest / RTL | 2 | 2 | 0 | **PASS** |
| **TypeScript Typecheck** | `tsc --noEmit` | - | **PASS** | 0 | **PASS** |

---

## 5. Next Milestone Plan: Milestone 4 — LangGraph AI Investigation & Hybrid RAG

1. Implement LangGraph multi-node state graph (Evidence Collector &rarr; Runbook Retriever &rarr; Hypothesis Generator &rarr; Guarded Verifier).
2. Set up pgvector embedding schema and vector storage for operational runbooks.
3. Implement PostgreSQL Full-Text Search (tsvector) as lexical retrieval component.
4. Implement Reciprocal Rank Fusion (RRF) combining dense vector search and lexical search.
5. Integrate MCP client inside the LangGraph workflow to call MCP tools (`get_recent_deployments`, `get_service_health`).
6. Enforce safety policy: remediation proposals must never auto-execute.
