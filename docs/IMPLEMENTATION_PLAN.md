# OpsPilot - Implementation Plan & Roadmap

This roadmap governs the milestone-by-milestone development of the **Distributed Enterprise Incident & FinOps Copilot**.

---

## Milestone Progress Matrix

| Milestone | Title | Focus Area | Status | Target Deliverables |
| :---: | :--- | :--- | :---: | :--- |
| **0** | **Workspace & Architecture Planning** | Planning & Monorepo Init | **IN PROGRESS** | Workspace check, skeleton, architecture docs, contracts, git repo |
| **1** | **Local Infrastructure & Skeletons** | Docker & Service Bootstrap | PENDING | Docker Compose (Postgres+pgvector, Kafka KRaft, Redis), health endpoints |
| **2** | **Java Control Plane & Incident Mgmt** | Core Backend & Multi-Tenancy | PENDING | Auth, JWT, RBAC, tenant isolation, alert webhook, outbox, JPA tests |
| **3** | **Kafka Event-Driven Integration** | Messaging Backbone | PENDING | Transactional outbox publisher, Kafka consumers, idempotency, DLQ, mock triage |
| **4** | **AI Investigation & RAG** | LangGraph & pgvector RAG | PENDING | LangGraph triage graph, LLM abstraction, hybrid search (pgvector + FTS), citations |
| **5** | **MCP Operational Tools** | Operational Evidence | PENDING | Read-only MCP server, deployment/metric tools, MCP client integration |
| **6** | **React Operations Dashboard** | Frontend SPA & SSE | PENDING | React+TS UI, incident overview/details, AI cards, FinOps view, SSE stream |
| **7** | **FinOps Intelligence** | Cost Analytics & Attribution | PENDING | CSV billing import, deterministic anomaly detection, incident/deploy correlation |
| **8** | **Production Hardening & Tracing** | Observability & Reliability | PENDING | OpenTelemetry tracing, structured logging, Redis cache/rate-limiting, CI workflow |
| **9** | **Portfolio & Demo Readiness** | Documentation & Verification | PENDING | Comprehensive docs, ER diagrams, demo dataset, interview prep guide |

---

## Detailed Milestone Execution Breakdown

### Milestone 0: Workspace & Architecture Planning (Current)
- [x] Inspect environment runtimes (Java 21, Python 3.13, Node 22, npm 10, Docker, Git).
- [x] Establish monorepo structure and directory trees.
- [x] Author comprehensive project documentation and architectural specifications.
- [x] Author versioned event contracts (JSON schemas) for Kafka integration.
- [x] Create root `.gitignore`, `.env.example`, `Makefile`, and `README.md`.
- [ ] Initialize clean Git repository and create initial local commit.
- [ ] Deliver Milestone 0 Checkpoint Report.

### Milestone 1: Local Infrastructure & Service Skeletons
- Docker Compose configuration:
  - PostgreSQL 16 with `pgvector` extension enabled.
  - Apache Kafka in KRaft mode (single-node, no Zookeeper dependency).
  - Redis 7 for cache and rate-limiting.
  - Health checks and dependency ordering.
- Skeleton application bootstrapping:
  - Java 21 Spring Boot 3.4 service with Maven wrapper and health endpoint (`/actuator/health`).
  - Python FastAPI AI service with `/health` and poetry/pip requirements.
  - React 18 + TypeScript + Vite frontend skeleton with build verification.
  - Python MCP server skeleton.
- Local startup commands and environment verification scripts.

### Milestone 2: Java Control Plane & Incident Management
- Multi-tenant data model: `organizations`, `users`, `incidents`, `alerts`, `audit_logs`.
- Spring Security with stateless JWT authentication, password hashing (BCrypt), and RBAC (`ADMIN`, `OPERATOR`, `VIEWER`).
- Server-side tenant isolation enforcement using `TenantContext`.
- Alert webhook API (`POST /api/v1/alerts/webhook`) with fingerprinting and deduplication.
- Incident lifecycle state machine (`OPEN`, `INVESTIGATING`, `IDENTIFIED`, `MONITORING`, `RESOLVED`).
- Transactional outbox table and repository.
- Unit and integration tests demonstrating tenant isolation and lifecycle transitions.

### Milestone 3: Kafka Event-Driven Integration
- Kafka topic initialization (`incident.alerts.v1`, `incident.triage.requested.v1`, `incident.triage.completed.v1`, `incident.triage.dlq.v1`).
- Spring Scheduled transactional outbox poller and Kafka producer.
- Python Kafka consumer for triage requests with idempotency checks.
- Python deterministic mock triage generator emitting triage completed events.
- Spring Boot Kafka consumer receiving triage results and updating incident records.
- Dead-Letter Queue (DLQ) retry policy and error handler.
- Integration tests demonstrating alert -> outbox -> kafka -> triage -> db.

### Milestone 4: AI Investigation & Hybrid RAG
- LangGraph state machine: Input validation -> Context normalisation -> Severity classification -> RAG retrieval -> Evidence synthesis -> Structured findings.
- LLM provider abstraction: Deterministic Mock Provider for automated tests + Configurable actual provider.
- PostgreSQL Hybrid RAG:
  - Markdown runbook ingestion and chunking.
  - Vector embeddings via pgvector.
  - Lexical full-text search (tsvector).
  - Reciprocal Rank Fusion (RRF) reranking.
- Structured output validation with Pydantic and explicit citation verification.

### Milestone 5: MCP & Operational Evidence
- Standalone read-only Model Context Protocol (MCP) server.
- Tools:
  - `get_recent_deployments(service, environment, limit)`
  - `get_deployment_details(deploymentId)`
  - `get_service_health(service, environment)`
  - `query_service_metrics(service, metricName, timeWindowMinutes)`
  - `get_incident_history(service, lookbackDays)`
- MCP Client integration in Python AI service.
- Verification tests ensuring AI triage workflow invokes MCP tools and synthesizes returned evidence.

### Milestone 6: React Dashboard Integration
- Enterprise React 18 + TypeScript dashboard.
- Views: Login, Incident List, Incident Details, AI Investigation & Evidence, Runbook Viewer, Deployment Timeline, FinOps Dashboard, Organization/User Admin.
- Real-time updates via Server-Sent Events (SSE).
- Human-in-the-loop action approval modal for proposed remediations.
- Frontend component tests.

### Milestone 7: FinOps Intelligence
- FinOps domain models: `cloud_costs`, `budgets`, `cost_anomalies`.
- CSV billing data ingestion pipeline with multi-currency tracking.
- Deterministic anomaly detection using rolling-window IQR and Z-scores.
- Cost attribution engine correlating cost spikes with deployment timestamps and incident scale-outs.
- FinOps analytics REST APIs and dashboard views.

### Milestone 8: Production Hardening & Observability
- OpenTelemetry instrumentation across Spring Boot, Kafka headers, Python services, and PostgreSQL.
- Distributed trace propagation via W3C TraceContext across Kafka messages.
- Structured JSON logging with correlation IDs.
- Redis caching for expensive analytical queries; Redis token-bucket rate limiting on public webhooks.
- Failure recovery testing (Kafka down, poison pill events, LLM timeout).
- GitHub Actions CI pipeline configuration.

### Milestone 9: Final Deployment & Portfolio Preparation
- Comprehensive documentation updates: ER diagrams, sequence diagrams, API documentation.
- Synthetic demo scenarios (e.g. Memory Leak Incident, Broken Canary Deployment, Uncontrolled Lambda Cloud Cost Spike).
- Reproducible end-to-end demonstration guide.
- Technical interview defence talking points and resume-ready accomplishment bullets.
