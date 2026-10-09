import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import documents from '../test/fixtures/documents.json'
import { jsonResponse } from '../test/render'

afterEach(() => {
  vi.unstubAllGlobals()
  window.history.pushState({}, '', '/')
})

const urlOf = (input: RequestInfo | URL): string =>
  typeof input === 'string' ? input : input instanceof URL ? input.href : input.url

function routeFetch(analyze: () => Promise<Response>) {
  return vi.fn<typeof fetch>((input) => {
    const url = urlOf(input)
    if (url.endsWith('/api/health')) return Promise.resolve(jsonResponse({ aiConfigured: true }))
    if (url.endsWith('/analyze')) return analyze()
    return Promise.resolve(jsonResponse([]))
  })
}

describe('upload → analysis flow', () => {
  it('uploads a report, shows progress and lands on the results page', async () => {
    const fetchMock = routeFetch(() => Promise.resolve(jsonResponse(documents)))
    vi.stubGlobal('fetch', fetchMock)
    window.history.pushState({}, '', '/analyze/report')
    render(<App />)

    const input = await screen.findByLabelText('Choose file')
    fireEvent.change(input, {
      target: { files: [new File(['%PDF-1.7'], 'report.pdf', { type: 'application/pdf' })] },
    })
    await userEvent.click(screen.getByRole('button', { name: /analy[sz]e/i }))

    expect(
      await screen.findByRole(
        'heading',
        { level: 1, name: documents.documentLabel },
        { timeout: 4000 },
      ),
    ).toBeInTheDocument()
    const analyzeCall = fetchMock.mock.calls.find(([u]) => urlOf(u).endsWith('/analyze'))
    expect(analyzeCall?.[1]?.body).toBeInstanceOf(FormData)
  })

  it('keeps the analyze button disabled until a file is chosen', async () => {
    vi.stubGlobal(
      'fetch',
      routeFetch(() => Promise.resolve(jsonResponse(documents))),
    )
    window.history.pushState({}, '', '/analyze/report')
    render(<App />)
    expect(await screen.findByRole('button', { name: /analy[sz]e/i })).toBeDisabled()
  })

  it('shows the friendly server error and stays on the upload step', async () => {
    vi.stubGlobal(
      'fetch',
      routeFetch(() =>
        Promise.resolve(
          jsonResponse({ code: 'AI_BUSY', userMessage: 'The AI service is busy right now.' }, 503),
        ),
      ),
    )
    window.history.pushState({}, '', '/analyze/medicine')
    render(<App />)
    fireEvent.change(await screen.findByLabelText('Choose file'), {
      target: { files: [new File(['x'], 'med.png', { type: 'image/png' })] },
    })
    await userEvent.click(screen.getByRole('button', { name: /analy[sz]e/i }))
    expect(await screen.findByText('The AI service is busy right now.')).toBeInTheDocument()
    expect(screen.getByLabelText('Choose file')).toBeInTheDocument()
  })
})
