# OpsPilot — Architecture & Technical Design Specification

OpsPilot is a production-grade, distributed multi-tenant Incident Management and FinOps Copilot. It automates root-cause analysis, coordinates safe remediations with human-in-the-loop controls, and correlates cloud financial spend spikes directly with microservice operational incidents.

---

## 1. System Architecture Overview

```
                                      +------------------------------------+
                                      |       Operations Dashboard         |
                                      |     (React 18 + TypeScript + Vite) |
                                      +-----------------+------------------+
                                                        |
                                                        | HTTPS / REST + JWT
                                                        v
+------------------------+             +----------------+-------------------+             +-----------------------+
|  Alert Ingestion       |             |         Control Plane              |             |      Redis 7          |
|  (Prometheus, Pager,   +------------>+   (Java 21 / Spring Boot 3.4.0)    +<----------->+ (Caching, Rate Limits |
|   Datadog Webhooks)    |             |  - Strict Multi-Tenant Isolation   |             |  & Sliding Windows)   |
+------------------------+             |  - Transactional Outbox Pattern    |             +-----------------------+
                                       |  - Deterministic FinOps Z-Scores   |
                                       +----------------+-------------------+
                                                        |
                                                        | Publishes to Kafka
                                                        v
                                       +----------------+-------------------+
                                       |    Apache Kafka KRaft Cluster      |
                                       | - incident.alerts.v1               |
                                       | - incident.triage.requested.v1     |
                                       | - incident.triage.completed.v1     |
                                       | - incident.triage.dlq.v1           |
                                       +----------------+-------------------+
                                                        ^
                                                        | Consumes & Produces
                                                        v
+------------------------+             +----------------+-------------------+
|  MCP Telemetry Server  |<------------+       AI Triage Engine             |
|  (FastAPI / Python)    |   HTTP      |       (Python 3.13 / LangGraph)    |
| - Recent Deployments   |  Bearer     |  - StateGraph Investigation Loop   |
| - Config Diff Analyzer |             |  - Hybrid RAG (BM25 + Semantic RRF)|
| - Service Health/P99   |             |  - RequiresApproval Safety Invariant|
+------------------------+             +------------------------------------+
```

---

## 2. Distributed Tracing & Correlation Pipeline

Every request, event, and telemetry lookup carries an enterprise trace correlation ID conforming to W3C `traceparent` standards.

```
Client Request (X-Correlation-Id)
      |
      v
+-------------------------------+
| Spring Boot Control Plane     | ---> Enriches MDC [correlationId] & Logs
| (TraceCorrelationFilter)      | ---> Produces Kafka Event with correlationId header
+---------------+---------------+
                |
                v (Kafka Message Header)
+---------------+---------------+
| Python AI Triage Engine       | ---> Propagates via contextvars across coroutines
| (trace_correlation middleware)| ---> Injects into outbound MCP Telemetry Client
+---------------+---------------+
                |
                v (HTTP X-Correlation-Id)
+---------------+---------------+
| Python MCP Telemetry Server   | ---> Logs tool invocation with matching correlationId
| (TraceCorrelationMiddleware)  | ---> Returns X-Correlation-Id in HTTP Response
+-------------------------------+
```

---

## 3. Incident Lifecycle State Transitions

Incidents follow a deterministic state machine strictly validated at the domain layer:

```
    +--------+
    |  OPEN  +----------------------+
    +---+----+                      |
        |                           |
        | acknowledge               |
        v                           |
+-------+-------+                   |
| INVESTIGATING |                   |
+---+-------+---+                   |
    |       |                       |
    |       | identify              |
    |       v                       |
    |   +---+----+                  | resolve
    |   |IDENTIF.|                  |
    |   +---+----+                  |
    |       |                       |
    |       | monitor               |
    |       v                       |
    |   +---+----+                  |
    +-->| MONITOR|                  |
        +---+----+                  |
            |                       |
            | resolve               |
            v                       v
        +---+-----------------------+---+
        |            RESOLVED           |
        +---------------+---------------+
                        |
                        | reopen
                        v
                    +---+----+
                    |  OPEN  |
                    +--------+
```

---

## 4. LangGraph Multi-Node Investigation Workflow

When an incident triage request arrives from Kafka, LangGraph executes an autonomous diagnostic DAG:

1. **`ingest_alert`**: Normalizes alert fingerprints, environment labels, and service metadata.
2. **`query_telemetry`**: Invokes read-only MCP server tools:
   - Fetches recent Git commits and deployment logs.
   - Computes deployment config diffs between active version and baseline.
   - Evaluates CPU, memory, and P99 latency anomalies.
3. **`retrieve_runbooks`**: Executes Hybrid RAG combining:
   - Lexical matching (BM25 / tokenized keyword index).
   - Dense semantic vector search (cosine similarity).
   - Reciprocal Rank Fusion (RRF with `k=60`) to yield optimal runbook snippets.
4. **`synthesize_hypotheses`**: Formulates root-cause hypotheses with confidence scoring ($0.0 - 1.0$) and citations.
5. **`generate_remediations`**: Synthesizes actionable remediation steps, strictly enforcing the **`requiresApproval: true`** safety invariant on all destructive operations (e.g. rollbacks, replica scaling).

---

## 5. FinOps Cloud Financial Anomaly Engine

Cloud costs are analyzed deterministically without LLM arithmetic to guarantee zero hallucinations:

- **Lookback Window**: Default 14-day rolling window per service and environment.
- **Statistical Z-Score Algorithm**:
  $$\mu = \frac{1}{N} \sum_{i=1}^{N} x_i$$
  $$\sigma = \sqrt{\frac{1}{N-1} \sum_{i=1}^{N} (x_i - \mu)^2}$$
  $$z = \frac{x_{\text{today}} - \mu}{\sigma}$$
- **Thresholds**:
  - $z \ge 3.0$ or spike $> 30\% \implies$ **CRITICAL** / **HIGH** anomaly.
  - Automatically correlates with active incident IDs if timestamps overlap with deployment regressions or retry storms.
