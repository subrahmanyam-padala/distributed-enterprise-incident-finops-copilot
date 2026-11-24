import React from 'react'
import { CostAnomaly } from '../types/finops'
import { Link } from 'react-router-dom'

interface Props {
  anomalies: CostAnomaly[]
}

export const FinOpsCard: React.FC<Props> = ({ anomalies }) => {
  if (anomalies.length === 0) {
    return (
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h3 className="text-sm font-bold text-gray-900 mb-1">FinOps Cloud Cost Intelligence</h3>
        <p className="text-xs text-gray-500">No statistically significant cloud spend anomalies detected.</p>
      </div>
    )
  }

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden mb-6">
      <div className="bg-gradient-to-r from-emerald-900 to-teal-900 text-white p-5 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-lg bg-emerald-600/50 flex items-center justify-center font-bold text-lg border border-emerald-400/30">
            $
          </div>
          <div>
            <h3 className="font-semibold text-lg flex items-center gap-2">
              FinOps Cloud Financial Intelligence
              <span className="text-xs bg-emerald-500/40 text-emerald-200 px-2 py-0.5 rounded border border-emerald-400/30">
                Statistical Z-Score
              </span>
            </h3>
            <p className="text-xs text-emerald-200">
              Deterministic Anomaly Detection &bull; Zero LLM Arithmetic
            </p>
          </div>
        </div>
        <div className="text-right">
          <div className="text-2xl font-bold text-amber-300">
            {anomalies.length}
          </div>
          <div className="text-xs text-emerald-200">Active Cost Anomalies</div>
        </div>
      </div>

      <div className="p-6 divide-y divide-gray-100">
        {anomalies.map((anom) => (
          <div key={anom.id} className="py-4 first:pt-0 last:pb-0">
            <div className="flex items-start justify-between gap-4 mb-2">
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-bold text-gray-900 text-sm">
                    {anom.serviceName}
                  </span>
                  <span className="text-xs px-2 py-0.5 rounded font-mono bg-red-100 text-red-800 font-semibold">
                    +{anom.percentageChange.toFixed(0)}% Spike
                  </span>
                  <span className="text-xs px-2 py-0.5 rounded font-mono bg-gray-100 text-gray-700">
                    z-score: {anom.zScore.toFixed(2)}
                  </span>
                </div>
                <p className="text-xs text-gray-600 mt-1 leading-relaxed">
                  {anom.explanation}
                </p>
              </div>

              <div className="text-right flex-shrink-0">
                <div className="text-lg font-bold text-gray-900">
                  ${anom.actualAmount.toFixed(2)}
                </div>
                <div className="text-xs text-gray-400">
                  Baseline: ${anom.expectedAmount.toFixed(2)}
                </div>
              </div>
            </div>

            {/* Financial Correlation */}
            <div className="flex items-center gap-3 mt-3 pt-2 text-xs text-gray-500 font-mono">
              {anom.correlatedIncidentId && (
                <Link
                  to={`/incidents/${anom.correlatedIncidentId}`}
                  className="text-blue-600 hover:underline flex items-center gap-1 font-sans font-medium"
                >
                  &rarr; Correlated Incident: {anom.correlatedIncidentId}
                </Link>
              )}
              {anom.correlatedDeploymentVersion && (
                <span className="bg-gray-100 px-2 py-0.5 rounded text-gray-700 font-sans">
                  Release: <strong>{anom.correlatedDeploymentVersion}</strong>
                </span>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
