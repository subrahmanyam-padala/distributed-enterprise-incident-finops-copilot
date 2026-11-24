import React, { useState } from 'react'
import { TriageReport, SuggestedRemediation, RunbookCitation, PotentialRootCause, SupportingEvidence } from '../types/incident'

interface Props {
  report: TriageReport
  onActionComplete?: () => void
}

export const TriageReportCard: React.FC<Props> = ({ report, onActionComplete }) => {
  const [approvedActions, setApprovedActions] = useState<Record<string, boolean>>({})
  const [rejectedActions, setRejectedActions] = useState<Record<string, boolean>>({})

  const parseArray = <T,>(val: string | T[]): T[] => {
    if (Array.isArray(val)) return val
    try {
      return JSON.parse(val)
    } catch {
      return []
    }
  }

  const observations = parseArray<string>(report.observations)
  const evidence = parseArray<SupportingEvidence>(report.supportingEvidence)
  const causes = parseArray<PotentialRootCause>(report.potentialRootCauses)
  const remediations = parseArray<SuggestedRemediation>(report.suggestedRemediations)
  const citations = parseArray<RunbookCitation>(report.runbookCitations)

  const handleApprove = (title: string) => {
    setApprovedActions((prev) => ({ ...prev, [title]: true }))
    setRejectedActions((prev) => ({ ...prev, [title]: false }))
    if (onActionComplete) onActionComplete()
  }

  const handleReject = (title: string) => {
    setRejectedActions((prev) => ({ ...prev, [title]: true }))
    setApprovedActions((prev) => ({ ...prev, [title]: false }))
    if (onActionComplete) onActionComplete()
  }

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden mb-6">
      {/* Header banner */}
      <div className="bg-gradient-to-r from-blue-900 to-indigo-900 text-white p-5 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-lg bg-blue-600/50 flex items-center justify-center font-bold text-lg border border-blue-400/30">
            AI
          </div>
          <div>
            <h3 className="font-semibold text-lg flex items-center gap-2">
              LangGraph AI Autonomous Investigation
              <span className="text-xs bg-blue-500/40 text-blue-200 px-2 py-0.5 rounded border border-blue-400/30">
                Hybrid RAG
              </span>
            </h3>
            <p className="text-xs text-blue-200">
              Triage ID: <span className="font-mono">{report.triageId}</span> &bull; Duration: {report.investigationDurationMs}ms
            </p>
          </div>
        </div>
        <div className="text-right">
          <div className="text-2xl font-bold text-emerald-400">
            {(report.confidenceScore * 100).toFixed(0)}%
          </div>
          <div className="text-xs text-blue-200">Confidence Score</div>
        </div>
      </div>

      <div className="p-6 space-y-6">
        {/* Observations */}
        {observations.length > 0 && (
          <div>
            <h4 className="text-sm font-semibold text-gray-900 mb-2 flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-blue-500"></span> Validated Observations
            </h4>
            <ul className="list-disc list-inside text-sm text-gray-700 space-y-1 bg-gray-50 p-3 rounded-lg border border-gray-100">
              {observations.map((obs, idx) => (
                <li key={idx} className="leading-relaxed">{obs}</li>
              ))}
            </ul>
          </div>
        )}

        {/* Potential Root Causes */}
        {causes.length > 0 && (
          <div>
            <h4 className="text-sm font-semibold text-gray-900 mb-2 flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-purple-500"></span> Potential Root Causes
            </h4>
            <div className="space-y-2">
              {causes.map((c, idx) => (
                <div key={idx} className="p-3 bg-purple-50/50 border border-purple-100 rounded-lg text-sm">
                  <div className="flex items-center justify-between mb-1">
                    <span className="font-semibold text-purple-900">{c.hypothesis}</span>
                    <span className="text-xs px-2 py-0.5 rounded bg-purple-100 text-purple-800 font-medium">
                      Likelihood: {c.likelihood}
                    </span>
                  </div>
                  <p className="text-gray-600 text-xs">{c.reasoning}</p>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Supporting Evidence */}
        {evidence.length > 0 && (
          <div>
            <h4 className="text-sm font-semibold text-gray-900 mb-2 flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-amber-500"></span> Operational Telemetry Evidence (MCP)
            </h4>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {evidence.map((ev, idx) => (
                <div key={idx} className="p-3 bg-amber-50/30 border border-amber-200/60 rounded-lg text-xs">
                  <div className="flex items-center justify-between font-mono text-gray-500 mb-1">
                    <span>{ev.source}</span>
                    <span className="bg-amber-100 text-amber-800 px-1.5 py-0.5 rounded">{ev.evidenceType}</span>
                  </div>
                  <p className="text-gray-800">{ev.description}</p>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Runbook Citations */}
        {citations.length > 0 && (
          <div>
            <h4 className="text-sm font-semibold text-gray-900 mb-2 flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-indigo-500"></span> Retrieved Runbook Citations
            </h4>
            <div className="space-y-2">
              {citations.map((cite, idx) => (
                <div key={idx} className="p-3 bg-indigo-50/40 border border-indigo-100 rounded-lg text-xs">
                  <div className="flex items-center justify-between font-semibold text-indigo-900 mb-1">
                    <span>{cite.title}</span>
                    <span className="text-indigo-600 font-mono">Score: {(cite.relevanceScore * 100).toFixed(0)}%</span>
                  </div>
                  <p className="text-gray-700 italic border-l-2 border-indigo-300 pl-2 my-1">{cite.snippet}</p>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Human-in-the-Loop Remediation Center */}
        {remediations.length > 0 && (
          <div className="border-t border-gray-100 pt-5">
            <div className="flex items-center justify-between mb-3">
              <h4 className="text-sm font-semibold text-gray-900 flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
                Remediation Action Center (Human-In-The-Loop Sign-Off)
              </h4>
              <span className="text-xs bg-red-50 text-red-700 border border-red-200 px-2 py-0.5 rounded font-medium">
                Mandatory Human Approval
              </span>
            </div>

            <div className="space-y-3">
              {remediations.map((rem, idx) => {
                const isApproved = approvedActions[rem.actionTitle]
                const isRejected = rejectedActions[rem.actionTitle]

                return (
                  <div
                    key={idx}
                    className={`p-4 rounded-xl border transition-all ${
                      isApproved
                        ? 'bg-emerald-50/80 border-emerald-300'
                        : isRejected
                        ? 'bg-red-50/60 border-red-300 opacity-60'
                        : 'bg-gray-50/70 border-gray-200 hover:border-gray-300'
                    }`}
                  >
                    <div className="flex items-start justify-between gap-4 mb-2">
                      <div>
                        <div className="font-semibold text-gray-900 text-sm flex items-center gap-2">
                          {rem.actionTitle}
                          <span
                            className={`text-xs px-2 py-0.5 rounded font-mono ${
                              rem.riskLevel === 'HIGH'
                                ? 'bg-red-100 text-red-800'
                                : rem.riskLevel === 'MEDIUM'
                                ? 'bg-amber-100 text-amber-800'
                                : 'bg-blue-100 text-blue-800'
                            }`}
                          >
                            Risk: {rem.riskLevel}
                          </span>
                        </div>
                        <p className="text-gray-600 text-xs mt-1">{rem.actionDescription}</p>
                      </div>

                      {/* Interactive Buttons */}
                      <div className="flex items-center gap-2 flex-shrink-0">
                        {isApproved ? (
                          <span className="text-xs font-semibold text-emerald-700 bg-emerald-100 px-3 py-1.5 rounded-lg border border-emerald-300 flex items-center gap-1">
                            ✓ Approved by Operator
                          </span>
                        ) : isRejected ? (
                          <span className="text-xs font-semibold text-red-700 bg-red-100 px-3 py-1.5 rounded-lg border border-red-300">
                            ✕ Rejected
                          </span>
                        ) : (
                          <>
                            <button
                              onClick={() => handleApprove(rem.actionTitle)}
                              className="px-3 py-1.5 text-xs font-medium text-white bg-emerald-600 hover:bg-emerald-700 rounded-lg shadow-sm transition"
                            >
                              Approve &amp; Sign
                            </button>
                            <button
                              onClick={() => handleReject(rem.actionTitle)}
                              className="px-3 py-1.5 text-xs font-medium text-gray-700 bg-white hover:bg-gray-100 border border-gray-300 rounded-lg transition"
                            >
                              Reject
                            </button>
                          </>
                        )}
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
