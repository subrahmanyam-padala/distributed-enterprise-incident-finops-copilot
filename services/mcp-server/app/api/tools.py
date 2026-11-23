"""
MCP operational tools skeleton router.
Provides read-only evidence for AI triage. Full implementation in Milestone 5.
"""
from fastapi import APIRouter, Header, HTTPException, Query
from pydantic import BaseModel
from typing import Optional
import uuid
from datetime import datetime, timedelta, timezone

router = APIRouter()


def _require_auth(authorization: Optional[str]) -> None:
    from app.config import settings
    expected = f"Bearer {settings.mcp_auth_token}"
    if authorization != expected:
        raise HTTPException(status_code=401, detail="Invalid MCP authentication token")


class ToolResponse(BaseModel):
    tool: str
    success: bool
    data: dict
    timestamp: str = datetime.now(timezone.utc).isoformat()


@router.get("/deployments/recent")
async def get_recent_deployments(
    service: str = Query(..., description="Service name to query"),
    environment: str = Query("production"),
    limit: int = Query(10, ge=1, le=50),
    authorization: Optional[str] = Header(None),
):
    """Returns the most recent deployments for a service. Read-only."""
    _require_auth(authorization)
    # Synthetic data — real implementation wired to database in Milestone 5
    return ToolResponse(
        tool="get_recent_deployments",
        success=True,
        data={
            "service": service,
            "environment": environment,
            "deployments": [
                {
                    "deploymentId": str(uuid.uuid4()),
                    "commitSha": "f89a2b1c3d4e5f6a7b8c9d0e1f2a3b4c",
                    "version": "v2.4.1",
                    "deployedAt": (datetime.now(timezone.utc) - timedelta(minutes=12)).isoformat(),
                    "deployedBy": "ci-system",
                    "status": "SUCCESS",
                    "changedFiles": 14,
                }
            ],
        },
    )


@router.get("/services/{service}/health")
async def get_service_health(
    service: str,
    environment: str = Query("production"),
    authorization: Optional[str] = Header(None),
):
    """Returns current health status of a service. Read-only."""
    _require_auth(authorization)
    return ToolResponse(
        tool="get_service_health",
        success=True,
        data={
            "service": service,
            "environment": environment,
            "status": "DEGRADED",
            "errorRate": 18.4,
            "p99LatencyMs": 2340,
            "checkedAt": datetime.now(timezone.utc).isoformat(),
        },
    )
