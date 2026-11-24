"""
Expanded MCP Server tests.
Verifies health endpoint, authentication, and all operational telemetry tools:
- get_recent_deployments
- get_deployment_diff
- get_service_health
- get_service_metrics
"""

from fastapi.testclient import TestClient
from app.main import app
from app.config import settings

client = TestClient(app)
AUTH_HEADERS = {"Authorization": f"Bearer {settings.mcp_auth_token}"}


def test_mcp_health_returns_up():
    response = client.get("/api/v1/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert data["service"] == "opspilot-mcp-server"


def test_mcp_deployments_requires_auth():
    response = client.get("/api/v1/tools/deployments/recent?service=checkout-service")
    assert response.status_code == 401


def test_mcp_deployments_success_with_auth():
    response = client.get(
        "/api/v1/tools/deployments/recent?service=checkout-service",
        headers=AUTH_HEADERS,
    )
    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    assert body["tool"] == "get_recent_deployments"
    assert "deployments" in body["data"]
    deployments = body["data"]["deployments"]
    assert len(deployments) >= 1
    assert deployments[0]["serviceName"] == "checkout-service"
    assert "commitSha" in deployments[0]


def test_mcp_deployment_diff():
    response = client.get(
        "/api/v1/tools/deployments/diff?service=checkout-service&base_version=v2.4.0&target_version=v2.4.1",
        headers=AUTH_HEADERS,
    )
    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    assert body["tool"] == "get_deployment_diff"
    data = body["data"]
    assert data["serviceName"] == "checkout-service"
    assert data["configChangesDetected"] is True
    assert len(data["changedFiles"]) > 0


def test_mcp_service_health():
    response = client.get(
        "/api/v1/tools/services/checkout-service/health",
        headers=AUTH_HEADERS,
    )
    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    assert body["tool"] == "get_service_health"
    data = body["data"]
    assert data["serviceName"] == "checkout-service"
    assert data["status"] == "DEGRADED"
    assert data["cpuUsagePercent"] > 80.0
    assert data["activeConnections"] > 0


def test_mcp_service_metrics():
    response = client.get(
        "/api/v1/tools/services/checkout-service/metrics?metric_name=p99_latency_ms&window_minutes=30",
        headers=AUTH_HEADERS,
    )
    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    assert body["tool"] == "get_service_metrics"
    data = body["data"]
    assert data["service"] == "checkout-service"
    assert len(data["dataPoints"]) >= 5
