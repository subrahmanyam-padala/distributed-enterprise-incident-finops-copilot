"""
Trace correlation middleware for the MCP Server.
Provides FastAPI middleware to propagate correlation IDs across distributed traces.
"""

import uuid
import logging
from contextvars import ContextVar
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response

logger = logging.getLogger(__name__)

# Context var for async-safe correlation ID propagation
mcp_correlation_id: ContextVar[str] = ContextVar("mcp_correlation_id", default="")


class TraceCorrelationMiddleware(BaseHTTPMiddleware):
    """
    FastAPI middleware that extracts or generates correlation IDs
    and injects them into response headers for distributed tracing.
    """

    async def dispatch(self, request: Request, call_next) -> Response:
        correlation_id = self._extract_correlation_id(request)
        mcp_correlation_id.set(correlation_id)

        logger.info(
            "MCP request: %s %s [correlationId=%s]",
            request.method,
            request.url.path,
            correlation_id,
        )

        response: Response = await call_next(request)
        response.headers["X-Correlation-Id"] = correlation_id

        logger.info(
            "MCP response: %s %s [status=%d, correlationId=%s]",
            request.method,
            request.url.path,
            response.status_code,
            correlation_id,
        )

        return response

    @staticmethod
    def _extract_correlation_id(request: Request) -> str:
        # Priority: X-Correlation-Id > traceparent > new UUID
        cid = request.headers.get("x-correlation-id")
        if cid:
            return cid

        traceparent = request.headers.get("traceparent")
        if traceparent:
            parts = traceparent.split("-")
            if len(parts) >= 2:
                return parts[1]

        return str(uuid.uuid4())


def get_current_correlation_id() -> str:
    """Get the correlation ID for the current request context."""
    return mcp_correlation_id.get() or str(uuid.uuid4())
