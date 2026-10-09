import { act, renderHook, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { MAX_RECORDING_MS, useSpeechRecognition } from './useSpeechRecognition'

class FakeRecorder {
  static last: FakeRecorder | null = null
  state = 'inactive'
  mimeType = 'audio/ogg;codecs=opus'
  ondataavailable: ((ev: { data: Blob }) => void) | null = null
  onstop: (() => void) | null = null
  constructor() {
    FakeRecorder.last = this
  }
  start() {
    this.state = 'recording'
  }
  stop() {
    this.state = 'inactive'
    this.ondataavailable?.({ data: new Blob(['voice'], { type: this.mimeType }) })
    this.onstop?.()
  }
}

function stubRecorder(getUserMedia: () => Promise<unknown>) {
  vi.stubGlobal('MediaRecorder', FakeRecorder)
  Object.defineProperty(navigator, 'mediaDevices', {
    value: { getUserMedia },
    configurable: true,
  })
}

afterEach(() => {
  vi.unstubAllGlobals()
  vi.useRealTimers()
})

describe('useSpeechRecognition without Web Speech (Firefox)', () => {
  it('records, transcribes with the backend and returns the text', async () => {
    const track = { stop: vi.fn() }
    stubRecorder(() => Promise.resolve({ getTracks: () => [track] }))
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ text: 'what is my sugar' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)
    const onResult = vi.fn()
    const { result } = renderHook(() => useSpeechRecognition('hi', onResult))
    expect(result.current.supported).toBe(true)
    act(() => result.current.start())
    await waitFor(() => expect(result.current.listening).toBe(true))
    act(() => result.current.stop())
    await waitFor(() => expect(onResult).toHaveBeenCalledWith('what is my sugar'))
    expect(track.stop).toHaveBeenCalled()
    const [reqUrl, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(String(reqUrl)).toContain('/api/voice/transcribe')
    const form = init.body as FormData
    expect(form.get('language')).toBe('hi')
    expect(form.get('audio')).toBeInstanceOf(Blob)
  })

  it('stops automatically after the maximum recording time', async () => {
    vi.useFakeTimers()
    stubRecorder(() => Promise.resolve({ getTracks: () => [] }))
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{}', { status: 500 })))
    const { result } = renderHook(() => useSpeechRecognition('en', vi.fn()))
    act(() => result.current.start())
    await act(async () => {
      await vi.advanceTimersByTimeAsync(0)
    })
    expect(result.current.listening).toBe(true)
    await act(async () => {
      await vi.advanceTimersByTimeAsync(MAX_RECORDING_MS + 10)
    })
    expect(result.current.listening).toBe(false)
    expect(FakeRecorder.last?.state).toBe('inactive')
  })

  it('reports a denied microphone', async () => {
    stubRecorder(() => Promise.reject(new Error('denied')))
    const { result } = renderHook(() => useSpeechRecognition('en', vi.fn()))
    act(() => result.current.start())
    await waitFor(() => expect(result.current.error).toBe('Microphone permission was denied.'))
    expect(result.current.listening).toBe(false)
  })
})
