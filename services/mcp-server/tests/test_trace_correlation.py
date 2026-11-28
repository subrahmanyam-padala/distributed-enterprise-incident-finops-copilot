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

def test_trace_correlation_extracts_w3c_traceparent():
    trace_id = "4bf92f3577b34da6a3ce929d0e0e4736"
    traceparent = f"00-{trace_id}-00f067aa0ba902b7-01"
    response = client.get("/api/v1/health", headers={"traceparent": traceparent})
    assert response.status_code == 200
    assert response.headers.get("x-correlation-id") == trace_id

