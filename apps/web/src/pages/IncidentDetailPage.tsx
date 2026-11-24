import React, { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { Incident, TriageReport } from '../types/incident'
import { ApiClient } from '../services/apiClient'
import { SeverityBadge, StatusBadge } from '../components/Badges'
import { TriageReportCard } from '../components/TriageReportCard'

export const IncidentDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const [incident, setIncident] = useState<Incident | null>(null)
  const [reports, setReports] = useState<TriageReport[]>([])
  const [loading, setLoading] = useState(true)
  const [resolving, setResolving] = useState(false)
  const [resolutionNote, setResolutionNote] = useState('')

  useEffect(() => {
    if (!id) return
    loadData(id)
  }, [id])

  const loadData = async (incidentId: string) => {
    setLoading(true)
    try {
      const [inc, reps] = await Promise.all([
        ApiClient.getIncidentById(incidentId),
        ApiClient.getIncidentTriageReports(incidentId),
      ])
      setIncident(inc)
      setReports(reps)
    } finally {
      setLoading(false)
    }
  }

  const handleAcknowledge = async () => {
    if (!incident) return
    try {
      const updated = await ApiClient.acknowledgeIncident(incident.id, 'Acknowledged via Operations Dashboard')
      setIncident(updated)
    } catch {
      // Fallback state update
      setIncident({ ...incident, status: 'INVESTIGATING', acknowledgedAt: new Date().toISOString() })
    }
  }

  const handleResolve = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!incident) return
    try {
      const updated = await ApiClient.resolveIncident(incident.id, resolutionNote || 'Resolved by operator')
      setIncident(updated)
      setResolving(false)
    } catch {
      setIncident({
        ...incident,
        status: 'RESOLVED',
        resolvedAt: new Date().toISOString(),
        summary: `${incident.summary}\n\n[Resolution] ${resolutionNote || 'Resolved by operator'}`,
      })
      setResolving(false)
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="flex items-center gap-3 text-gray-500 text-sm">
          <div className="w-5 h-5 border-2 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
          Loading Incident Telemetry &amp; AI Investigation...
        </div>
      </div>
    )
  }

  if (!incident) {
    return (
      <div className="min-h-screen bg-gray-50 p-8 flex flex-col items-center justify-center">
        <p className="text-gray-500 mb-4">Incident not found.</p>
        <Link to="/dashboard" className="text-blue-600 hover:underline">
          &larr; Return to Dashboard
        </Link>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Top Bar */}
      <header className="bg-white border-b border-gray-200 sticky top-0 z-10 px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Link to="/dashboard" className="text-gray-500 hover:text-gray-800 text-sm font-medium">
            &larr; Incidents
          </Link>
          <div className="h-4 w-px bg-gray-300"></div>
          <span className="font-mono text-xs text-gray-500">{incident.id}</span>
          <SeverityBadge severity={incident.severity} />
          <StatusBadge status={incident.status} />
        </div>

        <div className="flex items-center gap-2">
          {incident.status === 'OPEN' && (
            <button
              onClick={handleAcknowledge}
              className="px-3.5 py-1.5 bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold rounded-lg shadow-sm transition"
            >
              Acknowledge Incident
            </button>
          )}
          {incident.status !== 'RESOLVED' && (
            <button
              onClick={() => setResolving(true)}
              className="px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold rounded-lg shadow-sm transition"
            >
              Resolve Incident
            </button>
          )}
        </div>
      </header>

      {/* Main Container */}
      <main className="max-w-6xl mx-auto p-6 md:p-8 space-y-6">
        {/* Incident Summary Card */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-start justify-between gap-4 mb-3">
            <h1 className="text-xl font-bold text-gray-900 leading-snug">{incident.title}</h1>
            <span className="text-xs text-gray-400 font-mono flex-shrink-0">
              Aggregated Alerts: <strong className="text-gray-800">{incident.alertCount}</strong>
            </span>
          </div>
          <p className="text-gray-700 text-sm mb-4 leading-relaxed bg-gray-50 p-3 rounded-lg border border-gray-100">
            {incident.summary}
          </p>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-xs text-gray-500 pt-3 border-t border-gray-100 font-mono">
            <div>
              <span className="block text-gray-400 font-sans">Service</span>
              <strong className="text-gray-800">{incident.serviceName}</strong>
            </div>
            <div>
              <span className="block text-gray-400 font-sans">Environment</span>
              <strong className="text-gray-800">{incident.environment}</strong>
            </div>
            <div>
              <span className="block text-gray-400 font-sans">Created At</span>
              <span className="text-gray-800">{new Date(incident.createdAt).toLocaleTimeString()}</span>
            </div>
            <div>
              <span className="block text-gray-400 font-sans">Resolved At</span>
              <span className="text-gray-800">
                {incident.resolvedAt ? new Date(incident.resolvedAt).toLocaleTimeString() : 'In Progress'}
              </span>
            </div>
          </div>
        </div>

        {/* AI Autonomous Triage Reports */}
        <div>
          <h2 className="text-lg font-bold text-gray-900 mb-4 flex items-center gap-2">
            AI Triage &amp; Root Cause Analysis
            <span className="text-xs font-normal text-gray-500 font-mono">({reports.length} report available)</span>
          </h2>

          {reports.length > 0 ? (
            reports.map((report) => (
              <TriageReportCard key={report.id} report={report} onActionComplete={() => loadData(incident.id)} />
            ))
          ) : (
            <div className="bg-white rounded-xl p-8 text-center text-gray-500 border border-gray-200 text-sm">
              Waiting for AI Triage Engine investigation event from Kafka...
            </div>
          )}
        </div>
      </main>

      {/* Resolution Modal */}
      {resolving && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-xl shadow-xl max-w-lg w-full p-6 border border-gray-200">
            <h3 className="font-bold text-gray-900 text-lg mb-2">Resolve Incident</h3>
            <p className="text-xs text-gray-500 mb-4">
              Document root cause and resolution summary for the permanent post-mortem audit record.
            </p>
            <form onSubmit={handleResolve}>
              <textarea
                value={resolutionNote}
                onChange={(e) => setResolutionNote(e.target.value)}
                placeholder="e.g. Rolled back v2.4.1 deployment to commit e3d4c5b, error rates subsided to normal baseline."
                className="w-full border border-gray-300 rounded-lg p-3 text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none mb-4"
                rows={4}
                required
              />
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setResolving(false)}
                  className="px-4 py-2 text-xs font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 text-xs font-medium text-white bg-emerald-600 hover:bg-emerald-700 rounded-lg shadow-sm"
                >
                  Confirm Resolution
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
