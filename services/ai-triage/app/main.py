"""
OpsPilot AI Triage Engine - FastAPI Application Entry Point

This service is responsible for:
  - Consuming incident triage requests from Kafka (incident.triage.requested.v1)
  - Orchestrating LangGraph stateful investigation workflows
  - Executing Hybrid RAG (pgvector + Full-Text Search) runbook retrieval
  - Calling read-only MCP operational tools for deployment/health evidence
  - Publishing structured investigation findings to Kafka (incident.triage.completed.v1)
"""

from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api.health import router as health_router
from app.config import settings


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Manage application lifespan events (startup and shutdown)."""
    print(f"[OpsPilot AI Triage] Starting up — environment: {settings.app_env}")
    # Kafka consumer startup will be added in Milestone 3
    yield
    print("[OpsPilot AI Triage] Shutting down gracefully...")


app = FastAPI(
    title="OpsPilot AI Triage Engine",
    description=(
        "LangGraph-powered AI incident investigation service with "
        "Hybrid RAG runbook retrieval and MCP operational tool integration."
    ),
    version="0.1.0",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_allowed_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Register routers
app.include_router(health_router, prefix="/api/v1", tags=["health"])
