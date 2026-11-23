# OpsPilot - Build State & Progress Ledger

## Current Status Summary

- **Current Milestone**: Milestone 1 — Local Infrastructure & Service Skeletons
- **Status**: Completed (Local commit prepared on branch `main`)
- **Next Milestone**: Milestone 2 — Java Control Plane & Incident Management
- **Last Verified Date**: 2026-11-23

---

## 1. Development Environment Audit

| Tool | Detected Version | Target Specification | Status | Notes |
| :--- | :--- | :--- | :---: | :--- |
| **Operating System** | Windows (PowerShell) | Windows / Linux / macOS | OK | Native workspace |
| **Java JDK** | OpenJDK 21.0.10 LTS (Temurin) | Java 21 LTS | **PASS** | Verified with Spring Boot 3.4.0 |
| **Maven** | Apache Maven 3.9.9 | Maven 3.9+ / `mvnw` wrapper | **PASS** | Installed and verified via `mvnw.cmd` |
| **Python** | 3.13.0 | Python 3.11+ | **PASS** | FastAPI & pytest verified |
| **Node.js** | v22.20.0 | Node 20+ LTS | **PASS** | Vite, React 18, Vitest verified |
| **npm** | 10.9.3 | npm 10+ | **PASS** | 408 packages installed cleanly |
| **Docker Engine** | 29.7.2 | Docker Engine 24+ | **PASS** | Docker Compose configured |
| **Git** | 2.47.1.windows.2 | Git 2.40+ | **PASS** | Configured: `subrahmanyam-padala` / `subbhu35@gmail.com` |

---

## 2. Implemented Functionality (Milestone 1)

1. **Local Container Infrastructure (`docker-compose.yml`)**:
   - **PostgreSQL 16** with `pgvector/pgvector:pg16` image, enabling `uuid-ossp`, `vector`, and `pg_trgm` extensions via `infra/docker/postgres/init.sql`.
   - **Apache Kafka** in KRaft mode (`bitnami/kafka:3.7.0`) without ZooKeeper, configured with cluster ID and port 9092.
   - **Kafka Topic Initializer** (`kafka-init`) one-shot container automatically provisioning required topics: `incident.alerts.v1`, `incident.triage.requested.v1`, `incident.triage.completed.v1`, `incident.triage.dlq.v1`.
   - **Redis 7** (`redis:7.2-alpine`) with volume persistence and health checks.
2. **Java 21 Spring Boot 3.4 Control Plane Skeleton (`services/control-plane`)**:
   - Complete `pom.xml` configured with Spring Boot 3.4.0, Spring Data JPA, Spring Security, Flyway, Spring Kafka, JJWT, OpenAPI (Swagger UI), and Testcontainers.
   - `ControlPlaneApplication.java` entry point with `@EnableScheduling`.
   - Flyway database migration `V1__initial_schema.sql` covering `organizations`, `users`, `incidents`, `alerts`, `outbox_events`, `triage_reports`, `audit_logs`, and `processed_events`.
   - `HealthController` exposing `/api/v1/health`.
   - Spring Security configuration allowing public access to health and OpenAPI docs.
   - Swagger / OpenAPI 3.0 configuration with Bearer JWT scheme.
   - Verified with Maven wrapper `mvnw.cmd`.
3. **Python AI Triage Engine Skeleton (`services/ai-triage`)**:
   - FastAPI application entry point with CORS and lifespan handler.
   - Pydantic Settings configuration (`app/config.py`).
   - Liveness probe endpoint at `/api/v1/health` with uptime tracking.
   - Pinned `pyproject.toml` and `requirements.txt`.
4. **Python Model Context Protocol (MCP) Server Skeleton (`services/mcp-server`)**:
   - FastAPI service exposing read-only operational telemetry tools.
   - Token authentication middleware (`Bearer <mcp_token>`).
   - Read-only tools implemented: `get_recent_deployments`, `get_service_health`.
5. **React 18 + TypeScript Frontend Skeleton (`apps/web`)**:
   - Modern Vite + React 18 + TypeScript SPA with TailwindCSS.
   - React Router DOM routing (`/dashboard` and `/health`).
   - Service health checker view probing all backend services.
   - Vitest + React Testing Library integration tests.
6. **Documentation**:
   - `docs/LOCAL_DEVELOPMENT.md`: Comprehensive startup, execution, and port guide.

---

## 3. Files Created or Modified

- `docker-compose.yml`
- `infra/docker/postgres/init.sql`
- `services/control-plane/pom.xml`
- `services/control-plane/mvnw`
- `services/control-plane/mvnw.cmd`
- `services/control-plane/.mvn/wrapper/maven-wrapper.properties`
- `services/control-plane/src/main/java/com/opspilot/controlplane/ControlPlaneApplication.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/config/SecurityConfig.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/config/OpenApiConfig.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/health/HealthController.java`
- `services/control-plane/src/main/resources/application.yml`
- `services/control-plane/src/main/resources/db/migration/V1__initial_schema.sql`
- `services/control-plane/src/test/java/com/opspilot/controlplane/health/HealthControllerTest.java`
- `services/control-plane/src/test/resources/application-test.yml`
- `services/ai-triage/pyproject.toml`
- `services/ai-triage/requirements.txt`
- `services/ai-triage/app/main.py`
- `services/ai-triage/app/config.py`
- `services/ai-triage/app/api/health.py`
- `services/ai-triage/tests/test_health.py`
- `services/mcp-server/requirements.txt`
- `services/mcp-server/app/main.py`
- `services/mcp-server/app/config.py`
- `services/mcp-server/app/api/health.py`
- `services/mcp-server/app/api/tools.py`
- `services/mcp-server/tests/test_mcp.py`
- `apps/web/package.json`
- `apps/web/vite.config.ts`
- `apps/web/tsconfig.json`
- `apps/web/index.html`
- `apps/web/tailwind.config.cjs`
- `apps/web/postcss.config.cjs`
- `apps/web/src/main.tsx`
- `apps/web/src/App.tsx`
- `apps/web/src/index.css`
- `apps/web/src/pages/DashboardPage.tsx`
- `apps/web/src/pages/HealthPage.tsx`
- `apps/web/src/test/setup.ts`
- `apps/web/src/test/DashboardPage.test.tsx`
- `docs/LOCAL_DEVELOPMENT.md`
- `docs/BUILD_STATE.md`

---

## 4. Build and Test Results

- **Java Control Plane**:
  - `mvn test-compile`: **PASS** (4 Java sources + 1 test source compiled)
  - `mvn test`: **PASS** (`HealthControllerTest` 1 passed, 0 failures, 0 errors in 18.8s)
- **Python AI Triage Engine**:
  - `pytest tests/test_health.py`: **PASS** (2 passed in 1.17s)
- **Python MCP Server**:
  - `pytest tests/test_mcp.py`: **PASS** (4 passed in 1.07s)
- **React Frontend**:
  - `npm run type-check`: **PASS** (0 TypeScript errors)
  - `npm test`: **PASS** (`DashboardPage.test.tsx` 2 passed)
  - `npm run build`: **PASS** (Production bundle generated in 4.09s)

---

## 5. Known Limitations & Unresolved Defects

- Live Docker containers are not kept running continuously in the background during build turns to minimize host resource consumption. Local developers can start them with `docker compose up -d`.

---

## 6. Exact Continuation Instructions

Upon receiving the user command `CONTINUE`:
1. Verify `docs/BUILD_STATE.md` and git status.
2. Begin **Milestone 2**: Java Control Plane & Incident Management.
3. Implement Domain Models: Organization, User, Incident, Alert, AuditLog.
4. Implement JWT authentication service, filter chain, and password hashing.
5. Implement Role-Based Access Control (RBAC) with `@PreAuthorize` (`ADMIN`, `OPERATOR`, `VIEWER`).
6. Implement `TenantContext` and tenant-scoped data isolation.
7. Build Alert Ingestion Webhook endpoint (`POST /api/v1/alerts/webhook`) with fingerprinting and deduplication.
8. Implement Incident Lifecycle State Machine transitions.
9. Write unit and integration tests proving tenant isolation.
