export interface CostAnomaly {
  id: string
  organizationId: string
  serviceName: string
  environment: string
  detectionDate: string
  actualAmount: number
  expectedAmount: number
  percentageChange: number
  zScore: number
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW'
  correlatedIncidentId?: string | null
  correlatedDeploymentVersion?: string | null
  status: 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED'
  explanation: string
  detectedAt: string
}

export interface CloudCostSummary {
  date: string
  serviceName: string
  provider: string
  amount: number
  currency: string
}
