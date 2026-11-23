import { useEffect, useState } from 'react'

interface ServiceHealth {
  name: string
  url: string
  status: 'UP' | 'DOWN' | 'CHECKING'
}

const SERVICES: Omit<ServiceHealth, 'status'>[] = [
  { name: 'Control Plane', url: '/api/v1/health' },
  { name: 'AI Triage Engine', url: 'http://localhost:8000/api/v1/health' },
  { name: 'MCP Server', url: 'http://localhost:8001/api/v1/health' },
]

export default function HealthPage() {
  const [services, setServices] = useState<ServiceHealth[]>(
    SERVICES.map(s => ({ ...s, status: 'CHECKING' }))
  )

  useEffect(() => {
    SERVICES.forEach((svc, i) => {
      fetch(svc.url)
        .then(r => r.ok ? 'UP' : 'DOWN')
        .catch(() => 'DOWN')
        .then(status => {
          setServices(prev =>
            prev.map((s, idx) =>
              idx === i ? { ...s, status: status as 'UP' | 'DOWN' } : s
            )
          )
        })
    })
  }, [])

  const statusColor = (s: ServiceHealth['status']) =>
    s === 'UP' ? 'text-green-700 bg-green-50 border-green-200' :
    s === 'DOWN' ? 'text-red-700 bg-red-50 border-red-200' :
    'text-yellow-700 bg-yellow-50 border-yellow-200'

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-8">
      <div className="max-w-lg w-full bg-white rounded-2xl shadow-sm border border-gray-200 p-8">
        <h1 className="text-xl font-bold text-gray-900 mb-6">Service Health</h1>
        <div className="space-y-3">
          {services.map(svc => (
            <div key={svc.name}
              className={`flex items-center justify-between p-4 rounded-xl border ${statusColor(svc.status)}`}
            >
              <span className="font-medium text-sm">{svc.name}</span>
              <span className="text-xs font-semibold uppercase tracking-wide">{svc.status}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
