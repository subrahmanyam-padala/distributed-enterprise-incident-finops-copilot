/**
 * Placeholder Dashboard Page — Milestone 1 Skeleton.
 * Full dashboard implementation is in Milestone 6.
 */
export default function DashboardPage() {
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center p-8">
      <div className="max-w-2xl w-full bg-white rounded-2xl shadow-sm border border-gray-200 p-10">
        {/* Header */}
        <div className="flex items-center gap-4 mb-8">
          <div className="w-12 h-12 rounded-xl bg-blue-600 flex items-center justify-center">
            <span className="text-white text-xl font-bold">O</span>
          </div>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">OpsPilot</h1>
            <p className="text-sm text-gray-500">Distributed Enterprise Incident &amp; FinOps Copilot</p>
          </div>
        </div>

        {/* Status card */}
        <div className="bg-green-50 border border-green-200 rounded-xl p-5 mb-6">
          <div className="flex items-center gap-2 mb-1">
            <span className="w-2 h-2 bg-green-500 rounded-full inline-block"></span>
            <span className="text-sm font-semibold text-green-800">Frontend Running — Milestone 1</span>
          </div>
          <p className="text-sm text-green-700">
            React 18 + TypeScript + Vite skeleton is operational. Full dashboard arrives in Milestone 6.
          </p>
        </div>

        {/* Architecture milestones */}
        <div className="space-y-3">
          {[
            { label: 'Milestone 0: Architecture & Planning', done: true },
            { label: 'Milestone 1: Infrastructure & Skeletons', done: true },
            { label: 'Milestone 2: Java Control Plane', done: false },
            { label: 'Milestone 3: Kafka Event-Driven Integration', done: false },
            { label: 'Milestone 4: AI Investigation & RAG', done: false },
            { label: 'Milestone 5: MCP Operational Tools', done: false },
            { label: 'Milestone 6: React Dashboard', done: false },
            { label: 'Milestone 7: FinOps Intelligence', done: false },
            { label: 'Milestone 8: Observability & Hardening', done: false },
            { label: 'Milestone 9: Portfolio & Demo', done: false },
          ].map(({ label, done }) => (
            <div key={label} className="flex items-center gap-3 text-sm">
              <span className={`w-5 h-5 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0 ${done ? 'bg-green-500 text-white' : 'bg-gray-200 text-gray-500'}`}>
                {done ? '✓' : '·'}
              </span>
              <span className={done ? 'text-gray-800 font-medium' : 'text-gray-500'}>{label}</span>
            </div>
          ))}
        </div>

        <div className="mt-8 pt-6 border-t border-gray-100 text-xs text-gray-400">
          API: <code className="bg-gray-100 px-1 py-0.5 rounded">http://localhost:8080/api/v1</code>
          &nbsp;&nbsp;AI Engine: <code className="bg-gray-100 px-1 py-0.5 rounded">http://localhost:8000</code>
          &nbsp;&nbsp;MCP: <code className="bg-gray-100 px-1 py-0.5 rounded">http://localhost:8001</code>
        </div>
      </div>
    </div>
  )
}
