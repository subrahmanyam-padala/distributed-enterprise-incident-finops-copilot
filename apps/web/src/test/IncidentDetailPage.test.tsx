import { describe, it, expect } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { IncidentDetailPage } from '../pages/IncidentDetailPage'

describe('IncidentDetailPage', () => {
  it('renders incident details and AI Triage report with remediation actions', async () => {
    render(
      <MemoryRouter initialEntries={['/incidents/inc-chk-001']}>
        <Routes>
          <Route path="/incidents/:id" element={<IncidentDetailPage />} />
        </Routes>
      </MemoryRouter>
    )

    await waitFor(() => {
      const serviceMatches = screen.getAllByText(/checkout-service/i)
      expect(serviceMatches.length).toBeGreaterThanOrEqual(1)
      expect(screen.getByText(/LangGraph AI Autonomous Investigation/i)).toBeInTheDocument()
      expect(screen.getByText(/88%/i)).toBeInTheDocument()
      expect(screen.getByText(/Remediation Action Center/i)).toBeInTheDocument()
      expect(screen.getByText(/Scale checkout-service pod replicas/i)).toBeInTheDocument()
      expect(screen.getAllByText(/Approve & Sign/i).length).toBeGreaterThanOrEqual(1)
    })
  })
})
