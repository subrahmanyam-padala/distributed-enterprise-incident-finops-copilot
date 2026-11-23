"""
OpsPilot MCP Server - Model Context Protocol server providing
read-only operational data tools for the AI Triage Engine.

Tools exposed:
  - get_recent_deployments
  - get_deployment_details
  - get_service_health
  - query_service_metrics
  - get_incident_history
"""

from fastapi import FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from app.api.health import router as health_router
from app.api.tools import router as tools_router
from app.config import settings

app = FastAPI(
    title="OpsPilot MCP Server",
    description=(
        "Read-only Model Context Protocol (MCP) server providing operational "
        "evidence tools for AI-assisted incident triage. All tools are strictly "
        "non-destructive and require authentication."
    ),
    version="0.1.0",
    docs_url="/docs",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # MCP server is internal only; restricted at network level
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)

app.include_router(health_router, prefix="/api/v1", tags=["health"])
app.include_router(tools_router, prefix="/api/v1/tools", tags=["mcp-tools"])
