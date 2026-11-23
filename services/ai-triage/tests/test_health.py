"""
AI Triage health endpoint tests.
Uses FastAPI TestClient (sync) so no live services are required.
"""

from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)


def test_health_returns_up():
    response = client.get("/api/v1/health")
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "UP"
    assert body["service"] == "opspilot-ai-triage"
    assert "uptime_seconds" in body
    assert body["version"] == "0.1.0"


def test_health_has_valid_uptime():
    response = client.get("/api/v1/health")
    assert response.status_code == 200
    uptime = response.json()["uptime_seconds"]
    assert isinstance(uptime, (int, float))
    assert uptime >= 0
