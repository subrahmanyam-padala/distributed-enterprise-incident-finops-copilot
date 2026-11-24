"""
Event schemas and payload models for AI Triage Kafka message contracts.
"""

from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime, timezone
import uuid


class SupportingEvidence(BaseModel):
    source: str
    evidenceType: str  # METRIC, LOG, DEPLOYMENT, HEALTH_CHECK, RUNBOOK
    description: str
    timestamp: Optional[str] = None
    metricValue: Optional[float] = None
    threshold: Optional[float] = None


class PotentialRootCause(BaseModel):
    hypothesis: str
    likelihood: str  # HIGH, MEDIUM, LOW
    reasoning: str


class SuggestedRemediation(BaseModel):
    actionTitle: str
    actionDescription: str
    riskLevel: str  # LOW, MEDIUM, HIGH
    requiresApproval: bool = True  # Always requires human sign-off


class RunbookCitation(BaseModel):
    documentId: str
    title: str
    chunkId: str
    section: Optional[str] = None
    snippet: Optional[str] = None
    relevanceScore: float


class IncidentTriageCompletedPayload(BaseModel):
    incidentId: str
    triageId: str = Field(default_factory=lambda: str(uuid.uuid4()))
    investigationDurationMs: int = 1200
    observations: List[str]
    supportingEvidence: List[SupportingEvidence]
    potentialRootCauses: List[PotentialRootCause]
    suggestedRemediations: List[SuggestedRemediation]
    runbookCitations: List[RunbookCitation]
    confidenceScore: float = 0.85
    uncertainties: List[str] = Field(default_factory=list)
    additionalInvestigationSteps: List[str] = Field(default_factory=list)
    completedAt: str = Field(default_factory=lambda: datetime.now(timezone.utc).isoformat())


class EventEnvelope(BaseModel):
    eventId: str = Field(default_factory=lambda: str(uuid.uuid4()))
    eventType: str
    schemaVersion: str = "1.0.0"
    tenantId: str
    timestamp: str = Field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
    correlationId: Optional[str] = None
    payload: Dict[str, Any]
