export type IncidentSeverity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO'
export type IncidentStatus = 'OPEN' | 'INVESTIGATING' | 'IDENTIFIED' | 'MONITORING' | 'RESOLVED'

export interface Incident {
  id: string
  organizationId: string
  title: string
  severity: IncidentSeverity
  status: IncidentStatus
  serviceName: string
  environment: string
  alertCount: number
  summary: string
  resolvedAt?: string | null
  acknowledgedBy?: string | null
  acknowledgedAt?: string | null
  createdAt: string
  updatedAt?: string | null
}

export interface Alert {
  id: string
  incidentId: string
  organizationId: string
  externalAlertId?: string
  source: string
  serviceName: string
  severity: string
  message: string
  environment: string
  fingerprint: string
  receivedAt: string
}

export interface SupportingEvidence {
  source: string
  evidenceType: 'METRIC' | 'LOG' | 'DEPLOYMENT' | 'HEALTH_CHECK' | 'RUNBOOK'
  description: string
  timestamp?: string
  metricValue?: number
  threshold?: number
}

export interface PotentialRootCause {
  hypothesis: string
  likelihood: 'HIGH' | 'MEDIUM' | 'LOW'
  reasoning: string
}

export interface SuggestedRemediation {
  actionTitle: string
  actionDescription: string
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH'
  requiresApproval: boolean
}

export interface RunbookCitation {
  documentId: string
  title: string
  chunkId: string
  section?: string
  snippet?: string
  relevanceScore: number
}

export interface TriageReport {
  id: string
  incidentId: string
  organizationId: string
  triageId: string
  confidenceScore: number
  investigationDurationMs: number
  observations: string | string[]
  supportingEvidence: string | SupportingEvidence[]
  potentialRootCauses: string | PotentialRootCause[]
  suggestedRemediations: string | SuggestedRemediation[]
  runbookCitations: string | RunbookCitation[]
  uncertainties: string | string[]
  additionalSteps: string | string[]
  createdAt: string
}

export interface PagedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface ApiResponse<T> {
  success: boolean
  message?: string
  data: T
  timestamp?: string
}
