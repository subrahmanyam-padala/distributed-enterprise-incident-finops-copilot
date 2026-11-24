# OpsPilot - Build State & Progress Ledger

## Current Status Summary

- **Current Milestone**: Milestone 2 — Java Control Plane & Incident Management
- **Status**: Completed (Tests passed: 36/36, Local compilation verified)
- **Next Milestone**: Milestone 3 — Event-Driven Architecture & Kafka Pipeline
- **Last Verified Date**: 2026-11-24

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

## 2. Implemented Functionality (Milestone 2)

1. **Domain Model & Multi-Tenant Entities**:
   - `Organization`: Multi-tenant organization entity with slug, status, subscription plan.
   - `User`: Organization-scoped users with BCrypt-hashed passwords and RBAC roles (`ADMIN`, `OPERATOR`, `VIEWER`).
   - `Incident`: Incidents with status (`OPEN`, `INVESTIGATING`, `IDENTIFIED`, `MONITORING`, `RESOLVED`), severity (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `INFO`), and correlation counters.
   - `Alert`: Ingested operational alerts linked to incidents, storing raw source, fingerprint, labels, and JSON metadata.
   - `OutboxEvent`: Transactional Outbox pattern entity ensuring reliable at-least-once Kafka publication.
   - `AuditLog`: Immutable tenant audit trail recording administrative and operational actions.
2. **Multi-Tenant Security Architecture**:
   - `TenantContext`: `ThreadLocal` context storing verified `organizationId`, `userId`, `role`, and `email`.
   - `JwtTokenProvider`: HMAC-SHA256 JJWT generator and validator with tenant and role claims.
   - `JwtAuthenticationFilter`: Extracts Bearer token, populates `TenantContext` and Spring `SecurityContextHolder`, and safely cleans up in `finally`.
   - `SecurityConfig`: Configured stateless session management, BCrypt(12), role-based method security (`@EnableMethodSecurity`), and public endpoint matching.
3. **Alert Ingestion Pipeline & Deduplication (`AlertIngestionService`)**:
   - Ingests alerts via `POST /api/v1/alerts/webhook` (HTTP 202 Accepted).
   - Generates deterministic SHA-256 fingerprints across alert attributes.
   - 15-minute sliding deduplication window to prevent alert storms.
   - 1-hour active incident grouping window for identical service + environment.
   - Dynamic incident severity escalation when incoming alerts have higher severity.
   - Transactional outbox event insertion in the exact same DB transaction as incident/alert creation.
4. **Incident Lifecycle & State Transitions (`IncidentService`)**:
   - Strict state machine validation:
     - `OPEN` -> `INVESTIGATING`, `RESOLVED`
     - `INVESTIGATING` -> `IDENTIFIED`, `MONITORING`, `RESOLVED`
     - `IDENTIFIED` -> `MONITORING`, `RESOLVED`, `INVESTIGATING`
     - `MONITORING` -> `RESOLVED`, `INVESTIGATING`
     - `RESOLVED` -> `OPEN` (reopen)
   - Cross-tenant access controls: All incident reads, updates, acknowledgments, and resolutions verify tenant ownership and reject cross-tenant requests with HTTP 403.
   - Audit logging for every state transition and administrative action.
5. **Comprehensive Integration Test Suite**:
   - `HealthControllerTest`: Liveness and health endpoints (1 test).
   - `AuthControllerTest`: Registration, login, duplicate slug rejection, invalid credentials, and `/me` validation (9 tests).
   - `AlertWebhookControllerTest`: Ingestion, deduplication, incident grouping, outbox event generation, validation (8 tests).
   - `IncidentControllerTest`: Incident listing, get-by-id, acknowledgment, status transitions, resolution, and RBAC boundaries (12 tests).
   - `TenantIsolationTest`: Rigorous multi-tenant isolation tests verifying Organisation B cannot read, list, or mutate Organisation A's incidents (6 tests).
   - **Total test suite: 36 tests run, 36 passed, 0 failures, 0 errors.**

---

## 3. Files Created or Modified (Milestone 2)

- `services/control-plane/src/main/java/com/opspilot/controlplane/domain/enums/Role.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/domain/enums/IncidentSeverity.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/domain/enums/IncidentStatus.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/domain/enums/OutboxStatus.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/domain/entity/Organization.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/domain/entity/User.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/domain/entity/Incident.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/domain/entity/Alert.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/domain/entity/OutboxEvent.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/domain/entity/AuditLog.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/repository/OrganizationRepository.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/repository/UserRepository.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/repository/IncidentRepository.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/repository/AlertRepository.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/repository/OutboxEventRepository.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/repository/AuditLogRepository.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/security/TenantContext.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/security/UserPrincipal.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/security/JwtTokenProvider.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/security/JwtAuthenticationFilter.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/config/SecurityConfig.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/exception/ResourceNotFoundException.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/exception/TenantAccessDeniedException.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/exception/DuplicateResourceException.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/exception/AuthenticationFailedException.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/exception/ValidationException.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/exception/GlobalExceptionHandler.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/dto/common/ApiResponse.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/dto/common/PagedResponse.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/dto/auth/RegisterRequest.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/dto/auth/LoginRequest.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/dto/auth/AuthResponse.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/dto/auth/UserResponse.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/dto/auth/OrganizationResponse.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/dto/alert/AlertWebhookRequest.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/dto/alert/AlertResponse.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/dto/incident/IncidentResponse.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/dto/incident/IncidentAcknowledgeRequest.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/dto/incident/IncidentStatusUpdateRequest.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/dto/incident/IncidentResolveRequest.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/dto/audit/AuditLogResponse.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/service/AuditLogService.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/service/AuthService.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/service/AlertIngestionService.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/service/IncidentService.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/controller/AuthController.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/controller/AlertWebhookController.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/controller/IncidentController.java`
- `services/control-plane/src/main/java/com/opspilot/controlplane/controller/AuditLogController.java`
- `services/control-plane/src/test/java/com/opspilot/controlplane/auth/AuthControllerTest.java`
- `services/control-plane/src/test/java/com/opspilot/controlplane/alert/AlertWebhookControllerTest.java`
- `services/control-plane/src/test/java/com/opspilot/controlplane/incident/IncidentControllerTest.java`
- `services/control-plane/src/test/java/com/opspilot/controlplane/isolation/TenantIsolationTest.java`
- `docs/BUILD_STATE.md`

---

## 4. Verification & Test Execution Results

| Test Suite | Framework | Total Tests | Passed | Failed | Execution Time |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **Health Check** | JUnit 5 / Spring Boot Test | 1 | 1 | 0 | ~0.1 s |
| **Auth Controller** | JUnit 5 / Spring Boot Test | 9 | 9 | 0 | ~7.2 s |
| **Alert Webhook** | JUnit 5 / Spring Boot Test | 8 | 8 | 0 | ~8.4 s |
| **Incident Lifecycle** | JUnit 5 / Spring Boot Test | 12 | 12 | 0 | ~9.6 s |
| **Tenant Isolation** | JUnit 5 / Spring Boot Test | 6 | 6 | 0 | ~6.1 s |
| **Python AI Triage Health** | Pytest / TestClient | 2 | 2 | 0 | ~0.4 s |
| **Python MCP Tools** | Pytest / TestClient | 4 | 4 | 0 | ~0.5 s |
| **React Dashboard Tests** | Vitest / RTL | 2 | 2 | 0 | ~1.3 s |
| **React TypeScript Typecheck**| `tsc --noEmit` | - | **PASS** | 0 | ~2.5 s |
| **React Production Build** | Vite Build | - | **PASS** | 0 | ~4.1 s (166 kB) |

---

## 5. Next Milestone Plan: Milestone 3 — Event-Driven Architecture & Kafka Pipeline

1. Implement Outbox Publisher Scheduled Poller in Java Control Plane (`ScheduledOutboxPublisher`).
2. Integrate Spring Kafka producer with idempotence and transactional boundaries.
3. Implement Python AI Triage Kafka consumer group (`incident-triage-worker`).
4. Implement retry handler, exponential backoff, and Dead Letter Queue (`incident.triage.dlq.v1`).
5. Ingest triage requested events and emit mock triage completion events.
6. Verify end-to-end integration and trace context propagation.
