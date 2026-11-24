import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import DashboardPage from './pages/DashboardPage'
import { IncidentDetailPage } from './pages/IncidentDetailPage'
import HealthPage from './pages/HealthPage'

/**
 * Root Application Router.
 * Full operational routes:
 *  - /dashboard: Operational incident list with status filtering & metrics
 *  - /incidents/:id: Incident detail with AI Triage report & remediation center
 *  - /health: Infrastructure & backend services health probe
 */
function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/incidents/:id" element={<IncidentDetailPage />} />
        <Route path="/health" element={<HealthPage />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
