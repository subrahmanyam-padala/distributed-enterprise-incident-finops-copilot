import pytest
from app.middleware.trace_correlation import extract_correlation_id, set_correlation_id, get_correlation_id

def test_extract_correlation_id_explicit():
    headers = {"x-correlation-id": "trace-123"}
    assert extract_correlation_id(headers) == "trace-123"

def test_extract_correlation_id_traceparent():
    headers = {"traceparent": "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"}
    assert extract_correlation_id(headers) == "4bf92f3577b34da6a3ce929d0e0e4736"

def test_extract_correlation_id_generated():
    headers = {}
    cid = extract_correlation_id(headers)
    assert cid is not None
    assert len(cid) > 0

def test_contextvar_correlation_id():
    set_correlation_id("test-context-cid")
    assert get_correlation_id() == "test-context-cid"
