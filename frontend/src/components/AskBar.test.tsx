import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { axe } from '../test/axe'
import { jsonResponse, renderWithApp } from '../test/render'
import { AskBar } from './AskBar'

afterEach(() => vi.unstubAllGlobals())

const answer = {
  query: 'q',
  answer: 'Paracetamol is commonly used for fever and mild pain.',
  language: 'en',
  groundedFacts: [],
  safetyNotes: [],
  fromDocument: false,
  sources: [
    { id: 'who', name: 'WHO', title: 't', description: 'd', url: 'https://who.int', category: 'c' },
  ],
  emergency: false,
}

describe('AskBar', () => {
  it('asks a question and announces the answer with sources', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(answer))
    vi.stubGlobal('fetch', fetchMock)
    renderWithApp(<AskBar />)

    await userEvent.type(
      screen.getByRole('searchbox', { name: 'Ask a health question' }),
      'What is paracetamol?',
    )
    await userEvent.click(screen.getByRole('button', { name: 'Ask' }))

    expect(await screen.findByText(answer.answer)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /WHO/ })).toHaveAttribute('rel', 'noopener noreferrer')
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('switches the answer language when the user types in Kannada', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ ...answer, language: 'kn' }))
    vi.stubGlobal('fetch', fetchMock)
    renderWithApp(<AskBar />)

    await userEvent.type(
      screen.getByRole('searchbox', { name: 'Ask a health question' }),
      'ಮಧುಮೇಹ ಎಂದರೇನು',
    )
    await userEvent.click(screen.getByRole('button', { name: 'Ask' }))

    await screen.findByText(answer.answer)
    const body = JSON.parse(
      (fetchMock.mock.calls[0] as [string, RequestInit])[1].body as string,
    ) as Record<string, unknown>
    expect(body.language).toBe('kn')
    expect(document.documentElement.lang).toBe('kn')
  })

  it('shows the emergency banner with 108 and 112 for red flags', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({ ...answer, emergency: true })))
    renderWithApp(<AskBar />)

    await userEvent.type(
      screen.getByRole('searchbox', { name: 'Ask a health question' }),
      'severe chest pain',
    )
    await userEvent.click(screen.getByRole('button', { name: 'Ask' }))

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('108')
    expect(screen.getByRole('link', { name: /Call 112/ })).toHaveAttribute('href', 'tel:112')
  })

  it('shows a friendly error when the API fails', async () => {
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValue(
          jsonResponse({ code: 'AI_QUOTA', userMessage: 'Daily limit reached.' }, 503),
        ),
    )
    renderWithApp(<AskBar />)
    await userEvent.type(screen.getByRole('searchbox', { name: 'Ask a health question' }), 'hello')
    await userEvent.click(screen.getByRole('button', { name: 'Ask' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Daily limit reached.')
  })

  it('has no detectable accessibility violations', async () => {
    const { container } = renderWithApp(<AskBar title="Ask ArogyaLens" />)
    expect(await axe(container)).toHaveNoViolations()
  })
})
