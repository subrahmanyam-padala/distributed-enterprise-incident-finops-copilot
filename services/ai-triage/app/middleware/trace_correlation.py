"""
Trace correlation middleware for the AI Triage Engine.
Propagates W3C traceparent and X-Correlation-Id headers for distributed tracing
across the OpsPilot control-plane, AI triage engine, and MCP server.
"""

import uuid
import logging
import contextvars
from typing import Optional

logger = logging.getLogger(__name__)

# Context variable to hold the current correlation ID across async boundaries
correlation_id_var: contextvars.ContextVar[Optional[str]] = contextvars.ContextVar(
    "correlation_id", default=None
)


def extract_correlation_id(headers: dict[str, str]) -> str:
    """
    Extract correlation ID from request headers.
    Priority: X-Correlation-Id > traceparent trace ID > new UUID.
    """
    # 1. Explicit correlation ID header
    correlation_id = headers.get("x-correlation-id") or headers.get("X-Correlation-Id")
    if correlation_id:
        return correlation_id

    # 2. W3C traceparent header (format: version-traceId-spanId-flags)
    traceparent = headers.get("traceparent")
    if traceparent:
        parts = traceparent.split("-")
        if len(parts) >= 2:
            return parts[1]

    # 3. Generate new UUID
    return str(uuid.uuid4())


def set_correlation_id(cid: str) -> None:
    """Set the correlation ID for the current async context."""
    correlation_id_var.set(cid)


def get_correlation_id() -> str:
    """Get the current correlation ID, or generate a new one."""
    cid = correlation_id_var.get()
    if cid is None:
        cid = str(uuid.uuid4())
        correlation_id_var.set(cid)
    return cid


class CorrelationIdFilter(logging.Filter):
    """
    Logging filter that injects the correlation ID into log records.
    Usage: add to logging config to get correlationId in all log output.
    """

    def filter(self, record: logging.LogRecord) -> bool:
        record.correlationId = get_correlation_id()  # type: ignore[attr-defined]
        return True


def configure_structured_logging() -> None:
    """
    Configure structured logging with correlation ID injection.
    Call once at application startup.
    """
    root_logger = logging.getLogger()
    correlation_filter = CorrelationIdFilter()
    root_logger.addFilter(correlation_filter)

    formatter = logging.Formatter(
        "%(asctime)s [%(levelname)s] [correlationId=%(correlationId)s] "
        "%(name)s - %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )

    for handler in root_logger.handlers:
        handler.setFormatter(formatter)
        handler.addFilter(correlation_filter)

    logger.info("Structured logging configured with correlation ID injection")
