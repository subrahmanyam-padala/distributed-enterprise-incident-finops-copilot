import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_trace_correlation_header_injected():
    response = client.get("/api/v1/health")
    assert response.status_code == 200
    assert "x-correlation-id" in response.headers
    assert len(response.headers["x-correlation-id"]) > 0

def test_trace_correlation_propagated():
    custom_id = "test-custom-trace-id-12345"
    response = client.get("/api/v1/health", headers={"X-Correlation-Id": custom_id})
    assert response.status_code == 200
    assert response.headers.get("x-correlation-id") == custom_id

