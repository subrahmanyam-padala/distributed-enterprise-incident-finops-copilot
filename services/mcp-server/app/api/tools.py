"""
Expanded MCP operational telemetry tools.
Provides strictly read-only evidence for AI incident triage:
- get_recent_deployments: recent release history for microservices
- get_deployment_diff: changelog, modified files, config changes between versions
- get_service_health: live pod status, error rate, CPU/Memory metrics
- get_service_metrics: time-series metrics for latency, CPU, and error rate
"""

import uuid
from datetime import datetime, timedelta, timezone
from typing import Optional, List
from fastapi import APIRouter, Header, HTTPException, Query

from app.schemas.tools import (
    ToolResponse,
    DeploymentRecord,
    DeploymentDiff,
    ServiceHealthTelemetry,
)
from app.config import settings

router = APIRouter()


def _require_auth(authorization: Optional[str]) -> None:
    expected = f"Bearer {settings.mcp_auth_token}"
    if authorization != expected:
        raise HTTPException(status_code=401, detail="Invalid MCP authentication token")


# Sample repository data for realistic tool responses
DEPLOYMENTS_DB = {
    "checkout-service": [
        DeploymentRecord(
            deploymentId="dep-chk-001",
            serviceName="checkout-service",
            environment="production",
            version="v2.4.1",
            commitSha="a9f1b2c",
            deployedAt=(datetime.now(timezone.utc) - timedelta(minutes=24)).isoformat(),
            deployedBy="ci-cd-bot",
            status="SUCCESS",
            changedFilesCount=8,
            commitMessage="feat: add new payment gateway fallback handler",
        ),
        DeploymentRecord(
            deploymentId="dep-chk-000",
            serviceName="checkout-service",
            environment="production",
            version="v2.4.0",
            commitSha="e3d4c5b",
            deployedAt=(datetime.now(timezone.utc) - timedelta(days=2)).isoformat(),
            deployedBy="engineer-alice",
            status="SUCCESS",
            changedFilesCount=15,
            commitMessage="chore: bump spring boot version to 3.4.0",
        ),
    ],
    "auth-service": [
        DeploymentRecord(
            deploymentId="dep-auth-001",
            serviceName="auth-service",
            environment="production",
            version="v1.8.2",
            commitSha="7c8d9e0",
            deployedAt=(datetime.now(timezone.utc) - timedelta(hours=2)).isoformat(),
            deployedBy="ci-cd-bot",
            status="SUCCESS",
            changedFilesCount=4,
            commitMessage="fix: JWT token cache expiration key handling",
        )
    ],
}


# -----------------------------------------------------------------------------
# Tool 1: get_recent_deployments
# -----------------------------------------------------------------------------
@router.get("/deployments/recent", response_model=ToolResponse)
async def get_recent_deployments(
    service: str = Query(..., description="Service name to query"),
    environment: str = Query("production"),
    limit: int = Query(10, ge=1, le=50),
    authorization: Optional[str] = Header(None),
):
    """Returns the most recent deployments for a service. Read-only."""
    _require_auth(authorization)

    records = DEPLOYMENTS_DB.get(service, [
        DeploymentRecord(
            deploymentId=f"dep-{service}-v1.0.0",
            serviceName=service,
            environment=environment,
            version="v1.0.0",
            commitSha="1a2b3c4",
            deployedAt=(datetime.now(timezone.utc) - timedelta(hours=4)).isoformat(),
            deployedBy="ci-system",
            status="SUCCESS",
            changedFilesCount=5,
            commitMessage="initial release",
        )
    ])

    return ToolResponse(
        tool="get_recent_deployments",
        success=True,
        data={
            "service": service,
            "environment": environment,
            "deployments": [r.model_dump() for r in records[:limit]],
        },
    )


# -----------------------------------------------------------------------------
# Tool 2: get_deployment_diff
# -----------------------------------------------------------------------------
@router.get("/deployments/diff", response_model=ToolResponse)
async def get_deployment_diff(
    service: str = Query(..., description="Service name"),
    base_version: str = Query("v2.4.0", description="Base stable release"),
    target_version: str = Query("v2.4.1", description="Suspect target release"),
    authorization: Optional[str] = Header(None),
):
    """Compares two versions and returns changelog, modified files, and config diffs. Read-only."""
    _require_auth(authorization)

    diff = DeploymentDiff(
        serviceName=service,
        baseVersion=base_version,
        targetVersion=target_version,
        commits=[
            {"sha": "a9f1b2c", "message": "feat: add new payment gateway fallback handler", "author": "developer-bob"},
            {"sha": "b8e2a1d", "message": "fix: update connection timeout from 5000 to 30000ms", "author": "developer-charlie"},
        ],
        changedFiles=[
            "src/main/resources/application.yml",
            "src/main/java/com/opspilot/client/PaymentGatewayClient.java",
            "pom.xml",
        ],
        configChangesDetected=True,
        dependenciesModified=False,
    )

    return ToolResponse(
        tool="get_deployment_diff",
        success=True,
        data=diff.model_dump(),
    )


# -----------------------------------------------------------------------------
# Tool 3: get_service_health
# -----------------------------------------------------------------------------
@router.get("/services/{service}/health", response_model=ToolResponse)
async def get_service_health(
    service: str,
    environment: str = Query("production"),
    authorization: Optional[str] = Header(None),
):
    """Returns current health status and telemetry for a service. Read-only."""
    _require_auth(authorization)

    # Correlate health based on service
    telemetry = ServiceHealthTelemetry(
        serviceName=service,
        environment=environment,
        status="DEGRADED",
        cpuUsagePercent=88.5,
        memoryUsagePercent=74.1,
        errorRatePercent=16.2,
        p99LatencyMs=2150.0,
        activeConnections=96,
        podReplicasTotal=4,
        podReplicasHealthy=3,
    )

    return ToolResponse(
        tool="get_service_health",
        success=True,
        data=telemetry.model_dump(),
    )


# -----------------------------------------------------------------------------
# Tool 4: get_service_metrics (Time series window)
# -----------------------------------------------------------------------------
@router.get("/services/{service}/metrics", response_model=ToolResponse)
async def get_service_metrics(
    service: str,
    metric_name: str = Query("p99_latency_ms", description="Metric name: cpu_percent, error_rate, p99_latency_ms"),
    window_minutes: int = Query(30, ge=5, le=120),
    authorization: Optional[str] = Header(None),
):
    """Returns aggregated time-series telemetry over a sliding window. Read-only."""
    _require_auth(authorization)

    now = datetime.now(timezone.utc)
    points = []
    # Generate 6 sample intervals
    for i in range(6, 0, -1):
        ts = (now - timedelta(minutes=i * (window_minutes // 6))).isoformat()
        val = 120.0 + (6 - i) * 350.0 if "latency" in metric_name else 5.0 + (6 - i) * 2.5
        points.append({"timestamp": ts, "value": val})

    return ToolResponse(
        tool="get_service_metrics",
        success=True,
        data={
            "service": service,
            "metricName": metric_name,
            "windowMinutes": window_minutes,
            "dataPoints": points,
        },
    )
