"""
Pydantic schemas and contract definitions for MCP Operational Tools.
Strictly read-only evidence retrieval for AI incident investigation.
"""

from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime, timezone


class DeploymentRecord(BaseModel):
    deploymentId: str
    serviceName: str
    environment: str
    version: str
    commitSha: str
    deployedAt: str
    deployedBy: str
    status: str  # SUCCESS, FAILED, ROLLED_BACK
    changedFilesCount: int = 0
    commitMessage: str = ""


class DeploymentDiff(BaseModel):
    serviceName: str
    baseVersion: str
    targetVersion: str
    commits: List[Dict[str, str]] = Field(default_factory=list)
    changedFiles: List[str] = Field(default_factory=list)
    configChangesDetected: bool = False
    dependenciesModified: bool = False


class MetricPoint(BaseModel):
    timestamp: str
    value: float


class ServiceHealthTelemetry(BaseModel):
    serviceName: str
    environment: str
    status: str  # HEALTHY, DEGRADED, CRITICAL
    cpuUsagePercent: float
    memoryUsagePercent: float
    errorRatePercent: float
    p99LatencyMs: float
    activeConnections: int
    podReplicasTotal: int = 3
    podReplicasHealthy: int = 3
    checkedAt: str = Field(default_factory=lambda: datetime.now(timezone.utc).isoformat())


class ToolResponse(BaseModel):
    tool: str
    success: bool
    data: Dict[str, Any]
    timestamp: str = Field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
