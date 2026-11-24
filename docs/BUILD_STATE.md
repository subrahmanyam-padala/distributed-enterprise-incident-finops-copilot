# OpsPilot - Build State & Progress Ledger

## Current Status Summary

- **Current Milestone**: Milestone 5 — MCP Operational Telemetry Tools
- **Status**: Completed (Tests passed: Python MCP 6/6, Python AI 9/9, Java 38/38, Web 2/2)
- **Next Milestone**: Milestone 6 — React Operations Dashboard
- **Last Verified Date**: 2026-11-24

---

## 1. Development Environment Audit

| Tool | Detected Version | Target Specification | Status | Notes |
| :--- | :--- | :--- | :---: | :--- |
| **Operating System** | Windows (PowerShell) | Windows / Linux / macOS | OK | Native workspace |
| **Java JDK** | OpenJDK 21.0.10 LTS (Temurin) | Java 21 LTS | **PASS** | Verified with Spring Boot 3.4.0 |
| **Maven** | Apache Maven 3.9.9 | Maven 3.9+ / `mvnw` wrapper | **PASS** | Installed and verified via `mvnw.cmd` |
| **Python** | 3.13.0 | Python 3.11+ | **PASS** | LangGraph, FastApi, Pydantic, aiokafka verified |
| **Node.js** | v22.20.0 | Node 20+ LTS | **PASS** | Vite, React 18, Vitest verified |
| **npm** | 10.9.3 | npm 10+ | **PASS** | 408 packages installed cleanly |
| **Docker Engine** | 29.7.2 | Docker Engine 24+ | **PASS** | Docker Compose configured |
| **Git** | 2.47.1.windows.2 | Git 2.40+ | **PASS** | Configured: `subrahmanyam-padala` / `subbhu35@gmail.com` |

---

## 2. Implemented Functionality (Milestone 5)

1. **Expanded MCP Operational Toolset (`services/mcp-server`)**:
   - `get_recent_deployments`: Queries chronological deployment records including version, commit SHA, author, status, and changed files.
   - `get_deployment_diff`: Compares two release tags/commits to identify changelog, modified files list, and detect configuration changes (`application.yml`, `pom.xml`).
   - `get_service_health`: Real-time service probe telemetry reporting CPU%, memory%, error rate%, p99 latency, and replica health.
   - `get_service_metrics`: Sliding window time-series metric aggregator for latency, error rate, and resource utilization.
2. **Pydantic Validation & Contract Safety**:
   - Strict input and output schemas: `DeploymentRecord`, `DeploymentDiff`, `ServiceHealthTelemetry`, and `ToolResponse`.
   - Bearer token authentication required on all operational endpoints.
   - Strictly read-only tools: no mutations or destructive executions permitted via MCP protocol.
3. **AI Triage McpClient Integration**:
   - Integrated full MCP client capabilities inside `services/ai-triage/app/services/mcp_client.py`.
   - LangGraph `collect_evidence` node now gathers comprehensive multi-source telemetry: live health metrics, deployment histories, and config diffs.
   - Standalone fallback mode preserved for offline CI/CD test execution.

---

## 3. Files Created or Modified (Milestone 5)

- `services/mcp-server/app/schemas/__init__.py`
- `services/mcp-server/app/schemas/tools.py`
- `services/mcp-server/app/api/tools.py`
- `services/mcp-server/tests/test_mcp.py`
- `services/ai-triage/app/services/mcp_client.py`
- `docs/BUILD_STATE.md`

---

## 4. Verification & Test Execution Results

| Test Suite | Framework | Total Tests | Passed | Failed | Status |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **Python MCP Server** | Pytest / TestClient | 6 | 6 | 0 | **PASS (0.65s)** |
| - *Health Check* | TestClient | 1 | 1 | 0 | **PASS** |
| - *Deployments Auth Check* | TestClient | 1 | 1 | 0 | **PASS** |
| - *Deployments Success* | TestClient | 1 | 1 | 0 | **PASS** |
| - *Deployment Diff* | TestClient | 1 | 1 | 0 | **PASS** |
| - *Service Health* | TestClient | 1 | 1 | 0 | **PASS** |
| - *Service Metrics* | TestClient | 1 | 1 | 0 | **PASS** |
| **Python AI Triage Engine** | Pytest | 9 | 9 | 0 | **PASS (29.29s)** |
| **Java Control Plane** | JUnit 5 / Spring Boot Test | 38 | 38 | 0 | **PASS** |
| **React Web Dashboard** | Vitest / RTL | 2 | 2 | 0 | **PASS (5.16s)** |
| **TypeScript Typecheck** | `tsc --noEmit` | - | **PASS** | 0 | **PASS** |

---

## 5. Next Milestone Plan: Milestone 6 — React Operations Dashboard

1. Build responsive Operations Dashboard layout with navigation bar, tenant selector, and live status pills.
2. Implement Incident Management view: table with severity badges, status filter, and pagination.
3. Implement Incident Detail view:
   - Incident timeline and alert grouping details.
   - AI Investigation Card displaying hypotheses, confidence score, and supporting evidence.
   - Runbook citations with expandable snippets.
   - Remediation proposals with explicit human-in-the-loop "Approve" / "Reject" interactive action buttons.
4. Implement Server-Sent Events (SSE) or polling for real-time dashboard updates.
5. Add unit and component tests with Vitest and React Testing Library.
