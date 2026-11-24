import { ApiResponse, Incident, PagedResponse, TriageReport, IncidentStatus } from '../types/incident'

const API_BASE_URL = 'http://localhost:8080/api/v1'

export class ApiClient {
  private static token: string | null = localStorage.getItem('opspilot_token') || 'mock-dev-token'

  public static setToken(token: string) {
    this.token = token
    localStorage.setItem('opspilot_token', token)
  }

  public static clearToken() {
    this.token = null
    localStorage.removeItem('opspilot_token')
  }

  private static getHeaders(): HeadersInit {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
    }
    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`
    }
    return headers
  }

  public static async getIncidents(status?: IncidentStatus, page = 0, size = 20): Promise<PagedResponse<Incident>> {
    const url = new URL(`${API_BASE_URL}/incidents`)
    if (status) url.searchParams.append('status', status)
    url.searchParams.append('page', page.toString())
    url.searchParams.append('size', size.toString())

    try {
      const res = await fetch(url.toString(), { headers: this.getHeaders() })
      if (res.ok) {
        const body: ApiResponse<PagedResponse<Incident>> = await res.json()
        return body.data
      }
    } catch {
      // Fallback for standalone demo/testing without active backend
    }

    // Default mock data for realistic UI experience
    return {
      content: [
        {
          id: 'inc-chk-001',
          organizationId: 'org-demo-1',
          title: '[CRITICAL] High error rate and latency spike on checkout-service (production)',
          severity: 'CRITICAL',
          status: 'INVESTIGATING',
          serviceName: 'checkout-service',
          environment: 'production',
          alertCount: 4,
          summary: 'HTTP 504 Gateway Timeouts exceeding 18% over 5m window.',
          createdAt: new Date(Date.now() - 1000 * 60 * 18).toISOString(),
        },
        {
          id: 'inc-auth-002',
          organizationId: 'org-demo-1',
          title: '[HIGH] Database connection pool exhaustion on auth-service',
          severity: 'HIGH',
          status: 'OPEN',
          serviceName: 'auth-service',
          environment: 'production',
          alertCount: 2,
          summary: 'HikariPool-1 - Connection is not available, request timed out after 30000ms.',
          createdAt: new Date(Date.now() - 1000 * 60 * 45).toISOString(),
        },
        {
          id: 'inc-rep-003',
          organizationId: 'org-demo-1',
          title: '[MEDIUM] Reporting service replica pod OOMKilled',
          severity: 'MEDIUM',
          status: 'RESOLVED',
          serviceName: 'reporting-service',
          environment: 'staging',
          alertCount: 1,
          summary: 'Container OOMKilled exit code 137 on batch report generation.',
          resolvedAt: new Date(Date.now() - 1000 * 60 * 120).toISOString(),
          createdAt: new Date(Date.now() - 1000 * 60 * 180).toISOString(),
        },
      ],
      page: 0,
      size: 20,
      totalElements: 3,
      totalPages: 1,
      last: true,
    }
  }

  public static async getIncidentById(id: string): Promise<Incident> {
    try {
      const res = await fetch(`${API_BASE_URL}/incidents/${id}`, { headers: this.getHeaders() })
      if (res.ok) {
        const body: ApiResponse<Incident> = await res.json()
        return body.data
      }
    } catch {
      // fallback
    }

    return {
      id,
      organizationId: 'org-demo-1',
      title: '[CRITICAL] High error rate and latency spike on checkout-service (production)',
      severity: 'CRITICAL',
      status: 'INVESTIGATING',
      serviceName: 'checkout-service',
      environment: 'production',
      alertCount: 4,
      summary: 'HTTP 504 Gateway Timeouts exceeding 18% over 5m window on payment endpoints.',
      createdAt: new Date(Date.now() - 1000 * 60 * 25).toISOString(),
    }
  }

  public static async getIncidentTriageReports(id: string): Promise<TriageReport[]> {
    try {
      const res = await fetch(`${API_BASE_URL}/incidents/${id}/triage`, { headers: this.getHeaders() })
      if (res.ok) {
        const body: ApiResponse<TriageReport[]> = await res.json()
        return body.data
      }
    } catch {
      // fallback
    }

    return [
      {
        id: 'rep-001',
        incidentId: id,
        organizationId: 'org-demo-1',
        triageId: 'triage-chk-8891',
        confidenceScore: 0.88,
        investigationDurationMs: 1240,
        observations: [
          'Elevated 504 gateway timeouts correlated with payment gateway integration latency',
          'Recent release v2.4.1 deployed 24 minutes ago by ci-cd-bot',
          'Service health reports CPU at 88.5% and active DB connections at 96/100',
        ],
        supportingEvidence: [
          {
            source: 'mcp-deployment-registry',
            evidenceType: 'DEPLOYMENT',
            description: 'Release v2.4.1 deployed at 2026-11-24T05:45:00Z by ci-cd-bot (8 files changed)',
          },
          {
            source: 'mcp-service-health',
            evidenceType: 'HEALTH_CHECK',
            description: 'checkout-service status DEGRADED, CPU: 88.5%, ErrorRate: 16.2%',
            metricValue: 88.5,
            threshold: 80.0,
          },
        ],
        potentialRootCauses: [
          {
            hypothesis: 'Regression in payment gateway fallback handler causing thread starvation',
            likelihood: 'HIGH',
            reasoning: 'Config change in application.yml bumped timeout from 5s to 30s, exhausting pool threads.',
          },
        ],
        suggestedRemediations: [
          {
            actionTitle: 'Scale checkout-service pod replicas',
            actionDescription: 'Increase replica count from 3 to 6 pods to alleviate immediate thread congestion.',
            riskLevel: 'LOW',
            requiresApproval: true,
          },
          {
            actionTitle: 'Roll back checkout-service release v2.4.1',
            actionDescription: 'Revert deployment to stable commit e3d4c5b (v2.4.0).',
            riskLevel: 'HIGH',
            requiresApproval: true,
          },
        ],
        runbookCitations: [
          {
            documentId: 'rb-high-cpu-latency-001',
            title: 'High CPU and Latency Spike Resolution',
            chunkId: 'chunk-cpu-01',
            section: 'Remediation Actions',
            snippet: 'Scale deployment replicas using Kubernetes HPA. If correlated with recent release, initiate deployment rollback with approval.',
            relevanceScore: 0.94,
          },
        ],
        uncertainties: ['Third-party payment vendor outage not definitively confirmed'],
        additionalSteps: [
          'Inspect OpenTelemetry distributed trace waterfall for payment spans',
          'Check host-level connection drop counters',
        ],
        createdAt: new Date(Date.now() - 1000 * 60 * 15).toISOString(),
      },
    ]
  }

  public static async acknowledgeIncident(id: string, note?: string): Promise<Incident> {
    const res = await fetch(`${API_BASE_URL}/incidents/${id}/acknowledge`, {
      method: 'POST',
      headers: this.getHeaders(),
      body: JSON.stringify({ note }),
    })
    if (!res.ok) throw new Error('Failed to acknowledge incident')
    const body: ApiResponse<Incident> = await res.json()
    return body.data
  }

  public static async resolveIncident(id: string, resolutionSummary: string): Promise<Incident> {
    const res = await fetch(`${API_BASE_URL}/incidents/${id}/resolve`, {
      method: 'POST',
      headers: this.getHeaders(),
      body: JSON.stringify({ resolutionSummary }),
    })
    if (!res.ok) throw new Error('Failed to resolve incident')
    const body: ApiResponse<Incident> = await res.json()
    return body.data
  }
}
