import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { axe } from './test/axe'
import App from './App'
import { jsonResponse } from './test/render'

beforeEach(() => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(jsonResponse({ status: 'ok', aiConfigured: true })),
  )
})
afterEach(() => vi.unstubAllGlobals())

describe('App shell', () => {
  it('renders landmarks, a skip link and the ask bar on the home page', async () => {
    render(<App />)
    expect(screen.getByRole('link', { name: 'Skip to main content' })).toHaveAttribute(
      'href',
      '#main',
    )
    expect(screen.getByRole('main')).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Main' })).toBeInTheDocument()
    expect(screen.getByRole('search')).toBeInTheDocument()
  })

  it('updates the document language when the user picks a language', async () => {
    render(<App />)
    const [picker] = screen.getAllByRole('combobox', { name: 'Language' })
    await userEvent.selectOptions(picker, 'ta')
    expect(document.documentElement.lang).toBe('ta')
  })

  it('home page has no detectable accessibility violations', async () => {
    const { container } = render(<App />)
    await screen.findByRole('search')
    expect(await axe(container)).toHaveNoViolations()
  })
})
