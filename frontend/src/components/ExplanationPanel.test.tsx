import { screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { jsonResponse, renderWithApp } from '../test/render'
import type { MedicalParameter } from '../types'
import { ExplanationPanel } from './ExplanationPanel'

const param: MedicalParameter = {
  name: 'Haemoglobin',
  value: '11.2',
  unit: 'g/dL',
  referenceRange: '12-15',
  status: 'OUTSIDE_RANGE',
  explanation: 'Haemoglobin carries oxygen in the blood.',
  simpleExplanation: 'Haemoglobin carries oxygen.',
  confidence: 0.9,
  lowConfidence: false,
}

afterEach(() => vi.unstubAllGlobals())

describe('ExplanationPanel translation', () => {
  it('shows the translation in the chosen language', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(jsonResponse({ translated: 'ಹಿಮೋಗ್ಲೋಬಿನ್ ಆಮ್ಲಜನಕ ಸಾಗಿಸುತ್ತದೆ.' })),
    )
    renderWithApp(<ExplanationPanel parameter={param} explainLang="kn" />)
    expect(await screen.findByText('ಹಿಮೋಗ್ಲೋಬಿನ್ ಆಮ್ಲಜನಕ ಸಾಗಿಸುತ್ತದೆ.')).toHaveAttribute(
      'lang',
      'kn',
    )
    expect(screen.queryByText(/Translation is not available/)).not.toBeInTheDocument()
  })

  it('says so when translation fails instead of silently showing English', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({ error: 'busy' }, 503)))
    renderWithApp(<ExplanationPanel parameter={param} explainLang="kn" />)
    await waitFor(() =>
      expect(screen.getByRole('status')).toHaveTextContent(/Translation is not available/),
    )
  })
})
