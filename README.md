# OpsPilot — Distributed Enterprise Incident & FinOps Copilot

[![OpsPilot CI](https://github.com/subrahmanyam-padala/distributed-enterprise-incident-finops-copilot/actions/workflows/ci.yml/badge.svg)](https://github.com/subrahmanyam-padala/distributed-enterprise-incident-finops-copilot/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.4.0](https://img.shields.io/badge/Spring_Boot-3.4.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Python 3.13](https://img.shields.io/badge/Python-3.13-blue.svg)](https://www.python.org/)
[![LangGraph](https://img.shields.io/badge/LangGraph-1.2-purple.svg)](https://github.com/langchain-ai/langgraph)
[![Kafka KRaft](https://img.shields.io/badge/Kafka-KRaft_3.7-black.svg)](https://kafka.apache.org/)
[![React 18](https://img.shields.io/badge/React-18-cyan.svg)](https://react.dev/)
[![Redis 7](https://img.shields.io/badge/Redis-7-red.svg)](https://redis.io/)

**OpsPilot** is an enterprise-grade, distributed AI incident triage and FinOps copilot. It correlates high-severity operational outages with cloud spend anomalies, autonomously investigates root causes using **LangGraph StateGraphs** and **Model Context Protocol (MCP)** telemetry tools, and suggests guarded remediations with strict **Human-in-the-Loop** verification.

---

## 🏛️ System Architecture

```
                       +---------------------------------------+
                       |    React 18 Operations Dashboard      |
                       | (TypeScript, Vite 6, TailwindCSS)     |
                       +-------------------+-------------------+
                                           |
                                           | HTTPS / REST (JWT Multi-Tenant)
                                           v
+-----------------------+      +-----------+-----------+      +-------------------------+
| Alert Ingestion Engine| ---> |  Java 21 Control Plane| <--> |         Redis 7         |
| (Prometheus / Pager)  |      |   (Spring Boot 3.4)   |      | (TTL Caching & Rate Lim)|
+-----------------------+      +-----------+-----------+      +-------------------------+
                                           |
                                           | Transactional Outbox Pattern
                                           v
                              +------------+------------+
                              |   Kafka KRaft Cluster   |
                              | - alerts.v1             |
                              | - triage.requested.v1   |
                              | - triage.completed.v1   |
                              | - triage.dlq.v1         |
                              +------------+------------+
                                           ^
                                           | aiokafka (JSON Schema validated)
                                           v
+-----------------------+      +-----------+-----------+
| Python MCP Server     | <--- | Python AI Triage Eng. |
| (FastAPI Tools)       |      | (LangGraph + RRF RAG) |
+-----------------------+      +-----------------------+
```

---

## 🚀 Key Architectural Pillars

### 1. Multi-Tenant Java 21 Control Plane
- **Strict Data Isolation**: Tenant IDs enforced at repository and controller boundaries (`TenantContext`).
- **Resilient Messaging**: Transactional Outbox Pattern ensures zero message loss between PostgreSQL and Kafka.
- **Distributed Caching & Rate Limiting**: Spring Data Redis sliding-window filter with graceful fail-open resilience.

### 2. Autonomous LangGraph Investigation Engine
- **Cyclic DAG Evaluation**: Orchestrated state machine executing telemetry queries, log analysis, and root-cause synthesis.
- **Hybrid RAG**: Combines tokenized BM25 lexical search with dense semantic embeddings scored using **Reciprocal Rank Fusion (RRF)**.
- **Safety Invariant**: All destructive suggestions (rollbacks, pod scaling) enforce `requiresApproval: true`.

### 3. Model Context Protocol (MCP) Read-Only Tools
- Exposes decoupled, read-only operational telemetry tools for recent deployments, git diffs, CPU/memory stats, and P99 metrics.
- Fast token-authenticated HTTP endpoints with distributed trace headers.

### 4. Deterministic FinOps Intelligence
- **Zero Hallucination Guarantee**: Spend anomalies evaluated using purely statistical Z-Scores ($z \ge 3.0$) and percentage spikes over a 14-day rolling window—no LLM arithmetic.
- **Incident Financial Correlation**: Correlates cloud billing spikes directly with active microservice incidents.

### 5. Distributed Observability
- Seamless W3C `traceparent` and `X-Correlation-Id` propagation across Spring Boot, Python async workers, and MCP tools via SLF4J MDC and `contextvars`.

---

## 🛠️ Quickstart & Local Setup

### Prerequisites
- Java 21+ & Maven
- Python 3.13+
- Node.js 22+ & npm
- Docker & Docker Compose

### 1. Start Infrastructure (PostgreSQL, Kafka, Redis)
```bash
docker compose up -d
```

### 2. Run the Java Control Plane
```bash
cd services/control-plane
./mvnw spring-boot:run
```

### 3. Run the Python AI Triage Engine
```bash
cd services/ai-triage
pip install -r requirements.txt
python -m app.main
```

### 4. Run the Python MCP Server
```bash
cd services/mcp-server
pip install -r requirements.txt
python -m uvicorn app.main:app --port 8000
```

### 5. Launch the Web Dashboard
```bash
cd apps/web
npm install
npm run dev
```
Open [http://localhost:5173](http://localhost:5173) to view the Operations Dashboard.

---

## 🧪 Comprehensive Verification

To run the automated end-to-end integration verification suite:
```bash
python scripts/verify_system.py
```

### Running Unit & Integration Test Suites
```bash
# Java Control Plane (44+ tests)
cd services/control-plane && ./mvnw test

# Python AI Triage (13 tests)
cd services/ai-triage && pytest

# Python MCP Server (8 tests)
cd services/mcp-server && pytest

# React Operations Dashboard (3 tests + TypeScript type-check)
cd apps/web && npm test -- --run && npm run type-check
```

---

## 📁 Repository Structure

```text
distributed-enterprise-incident-finops-copilot/
├── .github/workflows/ci.yml      # Multi-service GitHub Actions CI pipeline
├── apps/web/                     # React 18 + TypeScript + Vite operations dashboard
├── services/
│   ├── control-plane/            # Java 21 Spring Boot REST API, FinOps & Outbox
│   ├── ai-triage/                # Python 3.13 LangGraph & Hybrid RAG investigation engine
│   └── mcp-server/               # FastAPI read-only operational telemetry MCP server
├── scripts/verify_system.py      # End-to-end multi-service smoke verification script
├── sample-data/                  # Sample runbooks & FinOps cloud billing CSVs
├── docs/ARCHITECTURE.md          # System architecture & sequence diagrams
└── docker-compose.yml            # PostgreSQL 16 (pgvector), Kafka KRaft, Redis 7
```

---

## 📄 License
Apache License 2.0. Built by Naga Subrahmanyam.
