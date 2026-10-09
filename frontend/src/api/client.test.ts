import { afterEach, describe, expect, it, vi } from 'vitest'
import { jsonResponse } from '../test/render'
import { ApiError, searchDoctors, voiceQuery } from './client'

afterEach(() => vi.unstubAllGlobals())

describe('api client', () => {
  it('sends the question, language and optional session', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ answer: 'ok' }))
    vi.stubGlobal('fetch', fetchMock)

    await voiceQuery('What is HbA1c?', 'kn')

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toBe('/api/voice/query')
    expect(JSON.parse(init.body as string)).toEqual({ query: 'What is HbA1c?', language: 'kn' })
  })

  it('raises ApiError with the backend code and friendly message', async () => {
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValue(
          jsonResponse({ code: 'AI_BUSY', userMessage: 'The AI service is busy right now.' }, 503),
        ),
    )
    const err = await searchDoctors({
      specialty: 'Dentist',
      location: 'Pune',
      language: 'en',
    }).catch((e: unknown) => e)
    expect(err).toBeInstanceOf(ApiError)
    expect(err).toMatchObject({
      code: 'AI_BUSY',
      status: 503,
      message: 'The AI service is busy right now.',
    })
  })

  it('explains rate limiting even without a JSON body', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('slow down', { status: 429 })))
    await expect(voiceQuery('hi', 'en')).rejects.toThrow(/Too many requests/)
  })
})
