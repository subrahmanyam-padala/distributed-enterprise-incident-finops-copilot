import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import DashboardPage from '../pages/DashboardPage'

describe('DashboardPage', () => {
  it('renders the OpsPilot heading', () => {
    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    )
    expect(screen.getByText('OpsPilot')).toBeInTheDocument()
  })

  it('shows Milestone 1 as complete', () => {
    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    )
    expect(
      screen.getByText(/Milestone 1: Infrastructure & Skeletons/)
    ).toBeInTheDocument()
  })
})
