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
    const body = JSON.parse(
      (fetchMock.mock.calls[0] as [string, RequestInit])[1].body as string,
    ) as Record<string, unknown>
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

function stubDeviceVoice() {
  const synth = {
    speak: vi.fn(),
    cancel: vi.fn(),
    getVoices: () => [{ lang: 'en-IN', name: 'English India' }],
  }
  vi.stubGlobal('speechSynthesis', synth)
  vi.stubGlobal(
    'SpeechSynthesisUtterance',
    class {
      text: string
      lang = ''
      voice: unknown = null
      onend: (() => void) | null = null
      onerror: (() => void) | null = null
      constructor(text: string) {
        this.text = text
      }
    },
  )
  return synth
}

describe('AudioPlayer', () => {
  it('plays the natural cloud voice first', async () => {
    const synth = stubDeviceVoice()
    const play = vi.fn().mockResolvedValue(undefined)
    vi.stubGlobal(
      'Audio',
      class {
        onended: (() => void) | null = null
        onerror: (() => void) | null = null
        play = play
        pause = vi.fn()
      },
    )
    vi.stubGlobal('URL', { createObjectURL: () => 'blob:x', revokeObjectURL: vi.fn() })
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response(new Blob(['RIFF']), { status: 200 })),
    )
    renderWithApp(<AudioPlayer text="Hello there" language="hi" />)
    await userEvent.click(screen.getByRole('button'))
    await vi.waitFor(() => expect(play).toHaveBeenCalled())
    expect(synth.speak).not.toHaveBeenCalled()
    expect(screen.getByRole('button')).toHaveAttribute('aria-pressed', 'true')
  })

  it('falls back to the device voice when the cloud voice fails, and can stop', async () => {
    const synth = stubDeviceVoice()
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('busy', { status: 503 })))
    renderWithApp(<AudioPlayer text="Hello there" language="en" />)
    const button = screen.getByRole('button')
    await userEvent.click(button)
    await vi.waitFor(() => expect(synth.speak).toHaveBeenCalled())
    expect(button).toHaveAttribute('aria-pressed', 'true')
    await userEvent.click(button)
    expect(synth.cancel).toHaveBeenCalled()
  })
})
