# OpsPilot: Distributed Enterprise Incident & FinOps Copilot

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-21_LTS-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4+-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.11+-blue.svg)](https://www.python.org/)
[![React](https://img.shields.io/badge/React-18+-61DAFB.svg)](https://react.dev/)
[![Kafka](https://img.shields.io/badge/Apache_Kafka-KRaft-black.svg)](https://kafka.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16_+_pgvector-336791.svg)](https://www.postgresql.org/)

**OpsPilot** is an enterprise-grade distributed incident response and cloud financial operations (FinOps) copilot designed for production reliability and cloud cost intelligence.

It combines a high-throughput **Java 21 / Spring Boot control plane**, an asynchronous **Kafka event-driven messaging backbone**, and a **Python / LangGraph AI reasoning engine** with **Model Context Protocol (MCP)** operational tool inspection and **Hybrid RAG** (lexical + vector retrieval) to assist engineers with evidence-backed incident triage and automated cost anomaly attribution.

---

## Key Capabilities

1. **Multi-Tenant Control Plane**: Secure tenant isolation, role-based access control (RBAC), and transactional outbox for reliable event publication.
2. **Alert Ingestion & Incident Lifecycle**: High-performance webhook ingestion, fingerprint-based deduplication, and full incident state transition audit trails.
3. **Event-Driven Resilience**: Kafka KRaft architecture with versioned JSON schemas, idempotent consumers, dead-letter queuing (DLQ), and distributed trace context propagation.
4. **AI-Assisted Incident Triage**: LangGraph workflow orchestrating severity classification, hybrid runbook retrieval, MCP operational inspection, and evidence validation.
5. **Operational Evidence via MCP**: Safe, read-only Model Context Protocol servers querying deployment history, service metrics, and health states.
6. **Deterministic FinOps Intelligence**: CSV billing ingestion, mathematical anomaly detection, budget thresholds, and multi-dimensional cost correlation (deployment & incident attribution) without LLM arithmetic.
7. **Human-in-the-Loop Remediations**: All operational remediation actions require explicit operator review and confirmation—never automatic destructive execution.
8. **Real-Time Operations Dashboard**: Responsive React & TypeScript interface featuring live SSE updates, timeline visualizations, and FinOps analytics.

---

## System Architecture

```text
Monitoring / Alert Sources
          |
          v
Spring Boot Alert API (Control Plane)
          |
          v
PostgreSQL Incident + Transactional Outbox
          |
          v
Outbox Poller / Publisher
          |
          v
Apache Kafka (KRaft mode: incident.triage.requested.v1)
          |
          v
Python AI Triage Consumer (FastAPI + LangGraph)
          |
          +----------------------+
          |                      |
          v                      v
      Hybrid RAG             MCP Server
   (pgvector + FTS)    (Deployments, Health, Metrics)
          |                      |
          +----------+-----------+
                     |
                     v
       Evidence Validation & Synthesis
                     |
                     v
Kafka Event (incident.triage.completed.v1)
                     |
                     v
Spring Boot Consumer -> PostgreSQL -> React Dashboard (SSE)
```

---

## Monorepo Layout

```text
distributed-enterprise-incident-finops-copilot/
├── apps/
│   └── web/                   # React + TypeScript Vite frontend dashboard
├── services/
│   ├── control-plane/         # Java 21 Spring Boot core service
│   ├── ai-triage/             # Python FastAPI + LangGraph AI engine
│   └── mcp-server/            # Read-only Model Context Protocol server
├── contracts/
│   ├── events/                # Versioned Kafka event JSON schemas
│   └── schemas/               # Shared domain schemas and envelopes
├── infra/
│   ├── docker/                # Container configurations & Dockerfiles
│   ├── kafka/                 # Kafka topic init and schema configurations
│   └── observability/         # OpenTelemetry Collector & dashboards
├── sample-data/
│   ├── alerts/                # Synthetic alert payloads for ingestion testing
│   ├── runbooks/              # Markdown/text runbooks for RAG indexing
│   ├── deployments/           # Synthetic deployment logs and histories
│   └── finops/                # Synthetic cloud billing CSV datasets
├── scripts/                   # Developer automation and database seed scripts
├── docs/                      # Comprehensive architectural and portfolio documentation
├── docker-compose.yml         # Local development infrastructure
├── Makefile                   # Quick developer task runner
└── README.md
```

---

## Getting Started

Detailed step-by-step instructions for local development and milestone status can be found in:
- [Implementation Roadmap](docs/IMPLEMENTATION_PLAN.md)
- [System Architecture](docs/SYSTEM_ARCHITECTURE.md)
- [Technology Decisions](docs/TECHNOLOGY_DECISIONS.md)
- [Current Build State](docs/BUILD_STATE.md)
- [Event Contracts](docs/EVENT_CONTRACTS.md)

---

## License

This project is licensed under the Apache 2.0 License.
