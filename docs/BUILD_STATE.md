# OpsPilot - Build State & Progress Ledger

## Current Status Summary

- **Current Milestone**: Milestone 4 — LangGraph AI Investigation & Hybrid RAG
- **Status**: Completed (Tests passed: Python AI 9/9, Java 38/38, Python MCP 4/4, Web 2/2)
- **Next Milestone**: Milestone 5 — MCP Operational Telemetry Tools
- **Last Verified Date**: 2026-11-24

---

## 1. Development Environment Audit

| Tool | Detected Version | Target Specification | Status | Notes |
| :--- | :--- | :--- | :---: | :--- |
| **Operating System** | Windows (PowerShell) | Windows / Linux / macOS | OK | Native workspace |
| **Java JDK** | OpenJDK 21.0.10 LTS (Temurin) | Java 21 LTS | **PASS** | Verified with Spring Boot 3.4.0 |
| **Maven** | Apache Maven 3.9.9 | Maven 3.9+ / `mvnw` wrapper | **PASS** | Installed and verified via `mvnw.cmd` |
| **Python** | 3.13.0 | Python 3.11+ | **PASS** | LangGraph, LangChain, aiokafka, pytest verified |
| **Node.js** | v22.20.0 | Node 20+ LTS | **PASS** | Vite, React 18, Vitest verified |
| **npm** | 10.9.3 | npm 10+ | **PASS** | 408 packages installed cleanly |
| **Docker Engine** | 29.7.2 | Docker Engine 24+ | **PASS** | Docker Compose configured |
| **Git** | 2.47.1.windows.2 | Git 2.40+ | **PASS** | Configured: `subrahmanyam-padala` / `subbhu35@gmail.com` |

---

## 2. Implemented Functionality (Milestone 4)

1. **Operational Runbook Repository (`sample-data/runbooks/`)**:
   - `rb-high-cpu-latency.md`: CPU saturation, thread bottlenecks, latency spikes, horizontal scaling, and rollback guidance.
   - `rb-db-pool-exhaustion.md`: Hikari connection pool starvation, blocking query termination (`pg_terminate_backend`), and connection pool tuning.
   - `rb-oom-memory-leak.md`: JVM/Python heap exhaustion, `OOMKilled` (Exit 137), heap dump capture, and replica recycling.
2. **Hybrid RAG Retrieval Engine (`HybridRetriever`)**:
   - Dual-channel retrieval combining dense vector semantic similarity with lexical TF-IDF / Full-Text Search.
   - Reciprocal Rank Fusion (RRF with `k=60`) algorithm fusing rankings across dense semantic embeddings and lexical matches.
   - Normalized relevance scores and markdown snippet extraction for citation generation.
3. **Model Context Protocol (MCP) Client (`McpClient`)**:
   - HTTP Bearer authenticated client querying the MCP server tools (`/api/v1/tools/deployments` and `/api/v1/tools/health`).
   - Seamless fallback for offline and standalone execution modes to maintain reproducible test isolation.
4. **LangGraph Stateful Multi-Node Investigation Workflow (`investigation_graph`)**:
   - Graph Topology:
     `collect_evidence` &rarr; `retrieve_runbooks` &rarr; `generate_hypotheses` &rarr; `verify_guardrails` &rarr; `END`.
   - `collect_evidence`: Gathers live MCP health metrics (CPU %, error rate) and deployment history.
   - `retrieve_runbooks`: Queries Hybrid RAG engine with incident details and extracts top relevant runbooks.
   - `generate_hypotheses`: Correlates evidence and runbooks to formulate root-cause hypotheses with likelihood rankings.
   - `verify_guardrails`: Enforces mandatory enterprise safety invariants.
5. **Strict Enterprise Guardrails**:
   - Every suggested remediation action hardcodes and validates `requiresApproval: true`. Auto-execution of production changes is strictly prohibited.
   - Confidence scores are mathematically bounded within `[0.0, 1.0]`.

---

## 3. Files Created or Modified (Milestone 4)

- `sample-data/runbooks/rb-high-cpu-latency.md`
- `sample-data/runbooks/rb-db-pool-exhaustion.md`
- `sample-data/runbooks/rb-oom-memory-leak.md`
- `services/ai-triage/app/services/hybrid_retriever.py`
- `services/ai-triage/app/services/mcp_client.py`
- `services/ai-triage/app/workflows/investigation_graph.py`
- `services/ai-triage/app/services/triage_pipeline.py`
- `services/ai-triage/tests/test_investigation_graph.py`
- `docs/BUILD_STATE.md`

---

## 4. Verification & Test Execution Results

| Test Suite | Framework | Total Tests | Passed | Failed | Status |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **Python AI Triage Engine** | Pytest | 9 | 9 | 0 | **PASS (30.61s)** |
| - *Health & Liveness* | TestClient | 2 | 2 | 0 | **PASS** |
| - *Triage Pipeline & DLQ* | Pytest unit | 3 | 3 | 0 | **PASS** |
| - *Tokenizer & Lexical Search* | Pytest unit | 1 | 1 | 0 | **PASS** |
| - *Hybrid RAG & RRF Fusion* | Pytest unit | 1 | 1 | 0 | **PASS** |
| - *LangGraph Individual Nodes* | Pytest unit | 1 | 1 | 0 | **PASS** |
| - *LangGraph End-to-End Workflow*| Pytest unit | 1 | 1 | 0 | **PASS** |
| **Python MCP Server** | Pytest / TestClient | 4 | 4 | 0 | **PASS (0.58s)** |
| **Java Control Plane** | JUnit 5 / Spring Boot Test | 38 | 38 | 0 | **PASS** |
| **React Web Dashboard** | Vitest / RTL | 2 | 2 | 0 | **PASS (5.16s)** |
| **TypeScript Typecheck** | `tsc --noEmit` | - | **PASS** | 0 | **PASS** |

---

## 5. Next Milestone Plan: Milestone 5 — MCP Operational Tools Expansion

1. Extend MCP Server with real operational tooling for deployment changelog diffing and health metrics aggregation.
2. Implement schema validation for MCP input/output contracts.
3. Integrate live MCP container in `docker-compose.yml` for unified local stack demonstration.
