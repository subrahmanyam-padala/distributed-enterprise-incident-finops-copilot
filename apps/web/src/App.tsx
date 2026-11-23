import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import DashboardPage from './pages/DashboardPage'
import HealthPage from './pages/HealthPage'

/**
 * Root Application Router.
 * Routes expand per milestone:
 *  - Milestone 1: /dashboard (placeholder) + /health
 *  - Milestone 2: /login, /incidents, /incidents/:id
 *  - Milestone 6: Full operational dashboard
 */
function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/health" element={<HealthPage />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
