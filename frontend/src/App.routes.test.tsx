import { render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { axe } from './test/axe'
import { jsonResponse } from './test/render'

beforeEach(() => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse([])))
})
afterEach(() => {
  vi.unstubAllGlobals()
  window.history.pushState({}, '', '/')
})

const routes: [string, RegExp][] = [
  ['/analyze/report', /report/i],
  ['/analyze/medicine', /medicine/i],
  ['/analyze/prescription', /prescription/i],
  ['/analyze/discharge', /discharge/i],
  ['/ask', /ask/i],
  ['/doctors', /doctor/i],
  ['/history', /history/i],
  ['/does-not-exist', /not found|404/i],
]

describe('routes', () => {
  it.each(routes)('%s renders a titled, accessible page', async (path, title) => {
    window.history.pushState({}, '', path)
    const { container } = render(<App />)
    expect(await screen.findByRole('heading', { level: 1, name: title })).toBeInTheDocument()
    expect(await axe(container)).toHaveNoViolations()
  })
})
