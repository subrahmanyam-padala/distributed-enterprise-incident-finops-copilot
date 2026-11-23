"""
MCP Server tests.
Verifies health endpoint and read-only operational tools.
"""

from fastapi.testclient import TestClient
from app.main import app
from app.config import settings

client = TestClient(app)


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
        headers={"Authorization": f"Bearer {settings.mcp_auth_token}"},
    )
    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    assert body["tool"] == "get_recent_deployments"
    assert "deployments" in body["data"]
    assert len(body["data"]["deployments"]) > 0


def test_mcp_service_health_success_with_auth():
    response = client.get(
        "/api/v1/tools/services/checkout-service/health",
        headers={"Authorization": f"Bearer {settings.mcp_auth_token}"},
    )
    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    assert body["tool"] == "get_service_health"
    assert body["data"]["service"] == "checkout-service"
