# OpsPilot — BUILD_STATE.md

## Current Status Summary
| Milestone | Description | Status |
|-----------|------------|--------|
| 1 | Local Infrastructure & Skeletons | ✅ Complete |
| 2 | Java Control Plane & Incident Management | ✅ Complete |
| 3 | Event-Driven Architecture & Kafka Pipeline | ✅ Complete |
| 4 | LangGraph AI Investigation & Hybrid RAG | ✅ Complete |
| 5 | MCP Operational Telemetry Tools | ✅ Complete |
| 6 | React Operations Dashboard | ✅ Complete |
| 7 | FinOps Cloud Financial Intelligence | ✅ Complete |
| 8 | Distributed Observability, Redis Caching & Rate Limiting | 🔲 Next |
| 9 | End-to-End Verification, Documentation & Portfolio | 🔲 Pending |

## Dev Environment Audit
- **Java**: 21 (Eclipse Temurin) + Spring Boot 3.4.0
- **Python**: 3.13 + LangGraph 1.2.24 + LangChain 1.5.2 + aiokafka 0.15.0
- **Node.js**: 22 + React 18 + Vite 6 + Vitest 2 + TailwindCSS 4
- **Docker Compose**: PostgreSQL 16 (pgvector), Kafka KRaft, Redis 7
- **Build**: Maven Wrapper (Java), pip (Python), npm (Node)

## Implemented Functionality — Milestone 7: FinOps Cloud Financial Intelligence

### Backend (Java Spring Boot)
- **Flyway migration V2**: `cloud_cost_items` and `cost_anomalies` database tables
- **Domain entities**: `CloudCostItem`, `CostAnomaly` with JPA annotations
- **Repositories**: Spring Data JPA repos with JPQL date-range queries
- **FinOpsService**: Deterministic z-score anomaly detection (mean + standard deviation) over configurable lookback window; zero LLM arithmetic
- **FinOpsController**: REST endpoints — `GET /anomalies`, `GET /costs/daily`, `POST /anomalies/detect`, `POST /seed`
- **Integration test**: `FinOpsControllerTest` verifying anomaly detection pipeline end-to-end

### Frontend (React + TypeScript)
- **TypeScript types**: `CostAnomaly`, `CloudCostSummary` interfaces
- **FinOpsCard component**: Statistical anomaly card with z-score, percentage spike, incident correlation links, deployment version tags
- **Dashboard integration**: FinOps card between KPI metrics and incident table, parallel fetch with `Promise.all`
- **API client**: `getCostAnomalies()` and `getDailyCosts()` with mock fallbacks for standalone dev

### Sample Data
- `sample-data/finops/sample-cloud-costs.csv`: 30-row billing dataset across 5 services

## Files Created/Modified (Milestone 7)
```
services/control-plane/src/main/resources/db/migration/V2__finops_schema.sql
services/control-plane/src/main/java/com/opspilot/controlplane/domain/entity/CloudCostItem.java
services/control-plane/src/main/java/com/opspilot/controlplane/domain/entity/CostAnomaly.java
services/control-plane/src/main/java/com/opspilot/controlplane/repository/CloudCostItemRepository.java
services/control-plane/src/main/java/com/opspilot/controlplane/repository/CostAnomalyRepository.java
services/control-plane/src/main/java/com/opspilot/controlplane/dto/finops/CostAnomalyResponse.java
services/control-plane/src/main/java/com/opspilot/controlplane/dto/finops/CloudCostSummary.java
services/control-plane/src/main/java/com/opspilot/controlplane/service/FinOpsService.java
services/control-plane/src/main/java/com/opspilot/controlplane/controller/FinOpsController.java
services/control-plane/src/test/java/com/opspilot/controlplane/finops/FinOpsControllerTest.java
sample-data/finops/sample-cloud-costs.csv
apps/web/src/types/finops.ts
apps/web/src/components/FinOpsCard.tsx
apps/web/src/services/apiClient.ts (modified)
apps/web/src/pages/DashboardPage.tsx (modified)
```

## Test Results
| Suite | Tests | Result |
|-------|-------|--------|
| Java Control Plane (mvnw test) | 39 | ✅ All pass |
| Python AI Triage (pytest) | 8 | ✅ All pass |
| Python MCP Server (pytest) | 7 | ✅ All pass |
| React Frontend (vitest) | 3 | ✅ All pass |
| TypeScript type-check (tsc --noEmit) | — | ✅ Clean |
| Vite production build | — | ✅ Success (195.85 KB JS gzip 61.53 KB) |

## Next Milestone Plan — M8: Distributed Observability, Redis Caching & Rate Limiting
1. OpenTelemetry trace correlation IDs and structured logging (Java + Python)
2. Redis caching layer for incident lookups and health checks
3. API rate limiting middleware
4. Performance and resilience tests
5. BUILD_STATE.md update and Git commit
