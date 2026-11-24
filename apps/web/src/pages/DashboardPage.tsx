import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Incident, IncidentStatus } from '../types/incident'
import { CostAnomaly } from '../types/finops'
import { ApiClient } from '../services/apiClient'
import { SeverityBadge, StatusBadge } from '../components/Badges'
import { FinOpsCard } from '../components/FinOpsCard'

export default function DashboardPage() {
  const [incidents, setIncidents] = useState<Incident[]>([])
  const [anomalies, setAnomalies] = useState<CostAnomaly[]>([])
  const [statusFilter, setStatusFilter] = useState<IncidentStatus | 'ALL'>('ALL')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchData()
  }, [statusFilter])

  const fetchData = async () => {
    setLoading(true)
    try {
      const filter = statusFilter === 'ALL' ? undefined : statusFilter
      const [incRes, anomRes] = await Promise.all([
        ApiClient.getIncidents(filter),
        ApiClient.getCostAnomalies(),
      ])
      setIncidents(incRes.content)
      setAnomalies(anomRes)
    } finally {
      setLoading(false)
    }
  }

  const criticalCount = incidents.filter((i) => i.severity === 'CRITICAL' && i.status !== 'RESOLVED').length
  const activeCount = incidents.filter((i) => i.status !== 'RESOLVED').length
  const resolvedCount = incidents.filter((i) => i.status === 'RESOLVED').length

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      {/* Top Navigation */}
      <nav className="bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between sticky top-0 z-10">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-blue-600 flex items-center justify-center shadow-sm">
            <span className="text-white text-lg font-bold">O</span>
          </div>
          <div>
            <h1 className="text-lg font-bold text-gray-900 leading-none">OpsPilot</h1>
            <span className="text-xs text-gray-500">Incident &amp; FinOps Copilot</span>
          </div>
        </div>

        <div className="flex items-center gap-4 text-xs">
          <span className="flex items-center gap-1.5 text-gray-600 bg-gray-100 px-3 py-1.5 rounded-lg border border-gray-200">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
            Tenant: <strong className="text-gray-900">Enterprise Tenant A</strong>
          </span>
          <Link to="/health" className="text-gray-600 hover:text-gray-900 font-medium">
            System Health &rarr;
          </Link>
        </div>
      </nav>

      {/* Main Content Area */}
      <main className="max-w-7xl w-full mx-auto p-6 md:p-8 flex-1 space-y-6">
        {/* KPI Metric Summary Cards */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
            <div className="text-xs text-gray-500 font-medium uppercase mb-1">Active Incidents</div>
            <div className="text-2xl font-bold text-gray-900">{activeCount}</div>
            <div className="text-xs text-gray-400 mt-1">Requiring operator attention</div>
          </div>
          <div className="bg-white p-5 rounded-xl border border-red-200 bg-red-50/20 shadow-sm">
            <div className="text-xs text-red-600 font-semibold uppercase mb-1">Critical Severity</div>
            <div className="text-2xl font-bold text-red-700">{criticalCount}</div>
            <div className="text-xs text-red-500 mt-1">SLA target &lt; 15 mins</div>
          </div>
          <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
            <div className="text-xs text-gray-500 font-medium uppercase mb-1">Resolved Today</div>
            <div className="text-2xl font-bold text-emerald-700">{resolvedCount}</div>
            <div className="text-xs text-emerald-600 mt-1">Root causes verified</div>
          </div>
          <div className="bg-white p-5 rounded-xl border border-blue-200 bg-blue-50/20 shadow-sm">
            <div className="text-xs text-blue-600 font-semibold uppercase mb-1">AI Triage Automation</div>
            <div className="text-2xl font-bold text-blue-800">100%</div>
            <div className="text-xs text-blue-500 mt-1">LangGraph + Hybrid RAG</div>
          </div>
        </div>

        {/* FinOps Cloud Financial Intelligence */}
        <FinOpsCard anomalies={anomalies} />

        {/* Filter and Table Card */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          {/* Controls Bar */}
          <div className="p-4 border-b border-gray-200 flex flex-wrap items-center justify-between gap-3 bg-gray-50/50">
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold text-gray-500 uppercase mr-1">Filter Status:</span>
              {(['ALL', 'OPEN', 'INVESTIGATING', 'IDENTIFIED', 'MONITORING', 'RESOLVED'] as const).map((status) => (
                <button
                  key={status}
                  onClick={() => setStatusFilter(status)}
                  className={`px-3 py-1 text-xs rounded-lg font-medium transition ${statusFilter === status
                    ? 'bg-blue-600 text-white shadow-sm'
                    : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-100'
                    }`}
                >
                  {status}
                </button>
              ))}
            </div>

            <button
              onClick={fetchData}
              className="text-xs text-gray-600 hover:text-gray-900 border border-gray-300 bg-white px-3 py-1 rounded-lg shadow-sm"
            >
              ↻ Refresh
            </button>
          </div>

          {/* Table */}
          {loading ? (
            <div className="p-12 text-center text-sm text-gray-500 flex items-center justify-center gap-2">
              <div className="w-4 h-4 border-2 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
              Loading incidents...
            </div>
          ) : incidents.length === 0 ? (
            <div className="p-12 text-center text-sm text-gray-500">
              No incidents found matching the selected filter.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-gray-50 text-xs text-gray-500 uppercase font-semibold border-b border-gray-200">
                  <tr>
                    <th className="px-6 py-3">Severity</th>
                    <th className="px-6 py-3">Title &amp; Summary</th>
                    <th className="px-6 py-3">Service</th>
                    <th className="px-6 py-3">Status</th>
                    <th className="px-6 py-3">Alerts</th>
                    <th className="px-6 py-3">Created</th>
                    <th className="px-6 py-3 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {incidents.map((incident) => (
                    <tr key={incident.id} className="hover:bg-gray-50/70 transition">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <SeverityBadge severity={incident.severity} />
                      </td>
                      <td className="px-6 py-4 max-w-md">
                        <Link
                          to={`/incidents/${incident.id}`}
                          className="font-semibold text-gray-900 hover:text-blue-600 line-clamp-1 block"
                        >
                          {incident.title}
                        </Link>
                        <p className="text-xs text-gray-500 line-clamp-1 mt-0.5">{incident.summary}</p>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap font-mono text-xs text-gray-700">
                        {incident.serviceName}
                        <span className="block text-gray-400 font-sans text-[11px]">{incident.environment}</span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <StatusBadge status={incident.status} />
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap font-mono text-xs font-semibold text-gray-700">
                        {incident.alertCount}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-xs text-gray-500">
                        {new Date(incident.createdAt).toLocaleTimeString()}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right">
                        <Link
                          to={`/incidents/${incident.id}`}
                          className="text-xs font-medium text-blue-600 hover:text-blue-800 bg-blue-50 hover:bg-blue-100 px-3 py-1.5 rounded-lg border border-blue-200 transition"
                        >
                          View Triage &rarr;
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
