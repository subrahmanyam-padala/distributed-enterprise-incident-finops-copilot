import React from 'react'
import { IncidentSeverity, IncidentStatus } from '../types/incident'

export const SeverityBadge: React.FC<{ severity: IncidentSeverity }> = ({ severity }) => {
  const styles: Record<IncidentSeverity, string> = {
    CRITICAL: 'bg-red-100 text-red-800 border-red-200',
    HIGH: 'bg-orange-100 text-orange-800 border-orange-200',
    MEDIUM: 'bg-yellow-100 text-yellow-800 border-yellow-200',
    LOW: 'bg-blue-100 text-blue-800 border-blue-200',
    INFO: 'bg-gray-100 text-gray-800 border-gray-200',
  }

  return (
    <span className={`px-2.5 py-0.5 rounded-full text-xs font-semibold border ${styles[severity] || styles.INFO}`}>
      {severity}
    </span>
  )
}

export const StatusBadge: React.FC<{ status: IncidentStatus }> = ({ status }) => {
  const styles: Record<IncidentStatus, string> = {
    OPEN: 'bg-red-50 text-red-700 border-red-300',
    INVESTIGATING: 'bg-amber-50 text-amber-700 border-amber-300 animate-pulse',
    IDENTIFIED: 'bg-purple-50 text-purple-700 border-purple-300',
    MONITORING: 'bg-blue-50 text-blue-700 border-blue-300',
    RESOLVED: 'bg-emerald-50 text-emerald-700 border-emerald-300',
  }

  return (
    <span className={`px-2.5 py-0.5 rounded-md text-xs font-medium border ${styles[status] || styles.OPEN}`}>
      {status}
    </span>
  )
}
