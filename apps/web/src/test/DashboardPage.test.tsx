import { describe, it, expect } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import DashboardPage from '../pages/DashboardPage'

describe('DashboardPage', () => {
  it('renders OpsPilot operational heading and metrics', async () => {
    render(
      <BrowserRouter>
        <DashboardPage />
      </BrowserRouter>
    )

    expect(screen.getByText('OpsPilot')).toBeInTheDocument()
    expect(screen.getByText('Incident & FinOps Copilot')).toBeInTheDocument()
    expect(screen.getByText('Active Incidents')).toBeInTheDocument()
    expect(screen.getByText('Critical Severity')).toBeInTheDocument()
  })

  it('renders incident list with severity and status badges', async () => {
    render(
      <BrowserRouter>
        <DashboardPage />
      </BrowserRouter>
    )

    await waitFor(() => {
      const links = screen.getAllByText(/checkout-service/i)
      expect(links.length).toBeGreaterThanOrEqual(1)
      expect(screen.getAllByText(/CRITICAL/i).length).toBeGreaterThanOrEqual(1)
      expect(screen.getAllByText(/INVESTIGATING/i).length).toBeGreaterThanOrEqual(1)
    })
  })

  it('renders FinOps cloud cost anomaly card with z-score and spike', async () => {
    render(
      <BrowserRouter>
        <DashboardPage />
      </BrowserRouter>
    )

    await waitFor(() => {
      expect(screen.getByText('FinOps Cloud Financial Intelligence')).toBeInTheDocument()
      expect(screen.getAllByText(/z-score:\s*4\.82/i).length).toBeGreaterThanOrEqual(1)
      expect(screen.getByText(/\+428% Spike/i)).toBeInTheDocument()
    })
  })
})
