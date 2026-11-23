# OpsPilot - Architectural Decision Records (ADRs)

This document records the foundational architectural decisions made for **OpsPilot**, detailing the context, selected choice, rationale, and considered trade-offs.

---

## ADR 001: Monorepo Architecture with Independent Service Boundaries

- **Status**: Accepted
- **Context**: OpsPilot involves a Java control plane, a Python AI engine, a Model Context Protocol server, and a React frontend. We need maintainable development without multi-repository synchronization overhead.
- **Decision**: Adopt a modular monorepo structured into `apps/`, `services/`, `contracts/`, `infra/`, and `docs/`.
- **Rationale**:
  - Centralizes versioned contracts (`contracts/events/`) shared across Java and Python.
  - Simplifies local Docker orchestration and end-to-end integration testing.
  - Keeps each service independently buildable and deployable without tight coupling.
- **Consequences**: Requires disciplined `.gitignore` and build tooling to avoid cross-language contamination.

---

## ADR 002: Java 21 & Spring Boot 3.4 for the Control Plane

- **Status**: Accepted
- **Context**: The core control plane handles high-volume alert ingestion, multi-tenant security, transactional consistency, and outbox event polling.
- **Decision**: Select Java 21 LTS with Spring Boot 3.4+, Spring Data JPA, and Spring Security.
- **Rationale**:
  - Java 21 brings Virtual Threads (Project Loom) for high-concurrency I/O operations without reactive programming complexity.
  - Spring Boot provides battle-tested enterprise security, transactional management (`@Transactional`), and mature Kafka integration (`spring-kafka`).
  - Strong typing prevents runtime data corruption in multi-tenant financial and incident records.
- **Trade-offs**: Slightly higher memory baseline compared to Go or Rust, but mitigated by JVM 21 memory efficiencies and container optimizations.

---

## ADR 003: Apache Kafka in KRaft Mode for Asynchronous Messaging

- **Status**: Accepted
- **Context**: Incident alerts require reliable asynchronous processing for AI triage without blocking webhook ingestion.
- **Decision**: Use Apache Kafka in KRaft (Kafka Raft Metadata) mode rather than legacy Zookeeper-dependent Kafka.
- **Rationale**:
  - KRaft eliminates the operational overhead of running and maintaining an external Zookeeper cluster.
  - Provides enterprise-grade partition scalability, durable message retention, and standard consumer group rebalancing.
  - Native support for consumer offsets and dead-letter queue patterns.
- **Trade-offs**: Requires disciplined schema evolution management (addressed via versioned JSON schemas in `contracts/`).

---

## ADR 004: Transactional Outbox Pattern for Relational-to-Kafka Reliability

- **Status**: Accepted
- **Context**: When an alert creates or updates an incident in PostgreSQL, an event must be published to Kafka. Direct publishing inside the HTTP request introduces dual-write inconsistencies if the broker is temporarily unreachable or the database transaction rolls back.
- **Decision**: Implement the Transactional Outbox Pattern using an `outbox_events` table in PostgreSQL.
- **Rationale**:
  - Guarantees atomicity: the incident entity and outbox event are committed in the exact same relational transaction.
  - A dedicated background publisher polls outbox events with `SELECT ... FOR UPDATE SKIP LOCKED` and publishes to Kafka with retry logic.
  - Guarantees at-least-once delivery semantics without requiring distributed 2-Phase Commit (2PC) transactions.

---

## ADR 005: PostgreSQL with pgvector and Lexical Full-Text Search for Hybrid RAG

- **Status**: Accepted
- **Context**: Incident investigation requires retrieving operational runbooks matching alert symptoms.
- **Decision**: Use PostgreSQL 16 with the `pgvector` extension for semantic vector retrieval, combined with native PostgreSQL `tsvector` / `tsquery` for lexical search, fused via Reciprocal Rank Fusion (RRF).
- **Rationale**:
  - Prevents infrastructure sprawl: avoids deploying and maintaining a separate vector database (e.g., Pinecone, Milvus, Qdrant) and search cluster (Elasticsearch) during development.
  - PostgreSQL handles relational data, outbox events, and vector embeddings in a single resilient engine with unified backup and replication.
  - The lexical search abstraction will be cleanly decoupled so that a standalone BM25 engine can be plugged in later if required.
- **Trade-offs**: For multi-million document datasets, dedicated search clusters provide greater scale; for typical enterprise runbook libraries (thousands to tens of thousands of documents), PostgreSQL + pgvector provides exceptional performance with near-zero operational complexity.

---

## ADR 006: Python FastAPI and LangGraph for AI Investigation Engine

- **Status**: Accepted
- **Context**: AI triage requires orchestrating multi-step reasoning, tool invocations, RAG retrieval, and structured output generation.
- **Decision**: Build the AI service in Python using FastAPI and LangGraph.
- **Rationale**:
  - LangGraph provides a cyclical, stateful graph architecture ideal for multi-step investigations, evidence verification loops, and fallback paths.
  - Python has first-class support for LLM orchestration, embedding generation, and the Model Context Protocol (MCP) SDK.
  - FastAPI provides asynchronous HTTP handling, OpenAPI auto-documentation, and seamless Pydantic data validation.

---

## ADR 007: Read-Only Model Context Protocol (MCP) Server for Operational Inspection

- **Status**: Accepted
- **Context**: The AI triage engine must query runtime context (deployments, metrics, service health) to validate hypotheses without hallucinating.
- **Decision**: Expose operational telemetry through a dedicated, read-only MCP server.
- **Rationale**:
  - Standardizes tool contracts using Anthropic's open Model Context Protocol.
  - Enforces strict security boundaries: all MCP tools are strictly read-only (`get_recent_deployments`, `get_service_health`, `query_service_metrics`).
  - No tool is capable of executing mutations or destructive production actions.
- **Consequences**: AI reasoning is grounded in verifiable external facts; evidence is structurally captured in the investigation report.

---

## ADR 008: Deterministic Statistical Algorithms for FinOps Intelligence

- **Status**: Accepted
- **Context**: Cloud cost management requires anomaly detection, budget tracking, and incident/deployment cost correlation.
- **Decision**: All financial arithmetic, budget burn calculations, and anomaly detections MUST be performed by deterministic mathematical algorithms (rolling-window Interquartile Range / Z-score statistics), NOT by Large Language Models.
- **Rationale**:
  - LLMs are probabilistic token predictors and are fundamentally unsuited for financial accounting and arithmetic.
  - Deterministic algorithms provide 100% reproducible, auditable, and mathematically sound anomaly alerts.
  - The LLM is restricted to synthesizing human-readable explanations of statistically verified anomalies against recorded deployment logs.

---

## ADR 009: Human-in-the-Loop Operational Remediation Policy

- **Status**: Accepted
- **Context**: Incident response often suggests remediation steps (e.g. restart pod, rollback deployment, increase replica count).
- **Decision**: The system will never execute destructive or production-altering remediations automatically. All actions require authenticated human approval (`requiresApproval: true`).
- **Rationale**:
  - Prevents cascading operational failures caused by automated actions acting on erroneous alert floods or edge cases.
  - Preserves engineer agency and regulatory compliance.
