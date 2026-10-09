import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useEffect, useState } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useApp } from '../hooks/useApp'
import { axe } from '../test/axe'
import discharge from '../test/fixtures/discharge.json'
import documents from '../test/fixtures/documents.json'
import medicines from '../test/fixtures/medicines.json'
import prescriptions from '../test/fixtures/prescriptions.json'
import { jsonResponse, renderWithApp } from '../test/render'
import type { AnalysisResponse } from '../types'
import Results from './Results'

/** Puts a demo analysis (captured from the backend's demo mode) into context, then shows Results. */
function WithAnalysis({ data }: { data: AnalysisResponse }) {
  const { setAnalysis } = useApp()
  const [ready, setReady] = useState(false)
  useEffect(() => {
    setAnalysis(data)
    setReady(true)
  }, [data, setAnalysis])
  return ready ? <Results /> : null
}

beforeEach(() => vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({}))))
afterEach(() => vi.unstubAllGlobals())

const cases: [string, unknown][] = [
  ['lab report', documents],
  ['medicine', medicines],
  ['prescription', prescriptions],
  ['discharge summary', discharge],
]

describe('Results page', () => {
  it.each(cases)('renders a %s accessibly', async (_name, data) => {
    const { container } = renderWithApp(<WithAnalysis data={data as AnalysisResponse} />)
    expect(await screen.findByRole('heading', { level: 1 })).toBeInTheDocument()
    expect(screen.getAllByRole('link').length).toBeGreaterThan(0)
    expect(await axe(container)).toHaveNoViolations()
  })

  it('expands a finding with the keyboard-operable accordion', async () => {
    renderWithApp(<WithAnalysis data={documents as unknown as AnalysisResponse} />)
    const toggles = await screen.findAllByRole('button', { expanded: false })
    await userEvent.click(toggles[0])
    expect(toggles[0]).toHaveAttribute('aria-expanded', 'true')
  })
})
