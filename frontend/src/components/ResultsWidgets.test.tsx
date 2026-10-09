import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { jsonResponse, renderWithApp } from '../test/render'
import { AudioPlayer } from './AudioPlayer'
import { ChatPanel } from './ChatPanel'
import { DoctorQuestions } from './DoctorQuestions'
import { FamilySummary } from './FamilySummary'

afterEach(() => vi.unstubAllGlobals())

describe('ChatPanel', () => {
  it('sends a question about the document and logs the answer', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({
        answer: 'Your HbA1c is above the usual range; discuss it with your doctor.',
        fromDocument: [],
        generalInfo: [],
        aiExplanation: [],
        safetyNotes: [],
      }),
    )
    vi.stubGlobal('fetch', fetchMock)
    renderWithApp(<ChatPanel sessionId="session-123" />)

    await userEvent.type(screen.getByRole('textbox'), 'What does HbA1c mean?')
    await userEvent.click(screen.getByRole('button', { name: /send|ask/i }))

    expect(await screen.findByText(/HbA1c is above the usual range/)).toBeInTheDocument()
    const body = JSON.parse((fetchMock.mock.calls[0] as [string, RequestInit])[1].body as string)
    expect(body).toMatchObject({ sessionId: 'session-123', message: 'What does HbA1c mean?' })
    expect(screen.getByRole('log')).toBeInTheDocument()
  })
})

describe('sharing widgets', () => {
  it('copies the family summary and doctor questions to the clipboard', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    vi.stubGlobal('navigator', { ...navigator, share: undefined, clipboard: { writeText } })

    renderWithApp(
      <>
        <FamilySummary summary="Most values are fine; one needs a doctor visit." />
        <DoctorQuestions questions={['Should I repeat this test?', 'Do I need diet changes?']} />
      </>,
    )
    expect(screen.getByText('Should I repeat this test?')).toBeInTheDocument()
    for (const button of screen.getAllByRole('button', { name: /copy/i })) {
      await userEvent.click(button)
    }
    expect(writeText).toHaveBeenCalledWith('Most values are fine; one needs a doctor visit.')
    expect(writeText).toHaveBeenCalledTimes(2)
    expect((await screen.findAllByText(/Copied/)).length).toBeGreaterThan(0)
  })
})

describe('AudioPlayer', () => {
  it('reads text aloud with a device voice and can stop', async () => {
    const speak = vi.fn()
    const cancel = vi.fn()
    vi.stubGlobal('speechSynthesis', {
      speak,
      cancel,
      getVoices: () => [{ lang: 'en-IN', name: 'English India' }],
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    })
    vi.stubGlobal(
      'SpeechSynthesisUtterance',
      class {
        text: string
        lang = ''
        voice: unknown = null
        rate = 1
        onend: (() => void) | null = null
        onerror: (() => void) | null = null
        constructor(text: string) {
          this.text = text
        }
      },
    )
    renderWithApp(<AudioPlayer text="Hello there" language="en" />)
    const button = screen.getByRole('button')
    await userEvent.click(button)
    expect(speak).toHaveBeenCalled()
    expect(button).toHaveAttribute('aria-pressed', 'true')
    await userEvent.click(button)
    expect(cancel).toHaveBeenCalled()
  })
})
