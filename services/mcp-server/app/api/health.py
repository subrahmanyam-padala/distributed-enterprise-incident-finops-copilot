"""MCP Server health router."""
import time
from fastapi import APIRouter
from pydantic import BaseModel

router = APIRouter()
_start_time = time.time()


class HealthResponse(BaseModel):
    status: str
    service: str
    version: str
    uptime_seconds: float


@router.get("/health", response_model=HealthResponse)
async def health_check() -> HealthResponse:
    return HealthResponse(
        status="UP",
        service="opspilot-mcp-server",
        version="0.1.0",
        uptime_seconds=round(time.time() - _start_time, 2),
    )
