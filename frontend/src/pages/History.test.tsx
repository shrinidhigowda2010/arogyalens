import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { axe } from '../test/axe'
import { jsonResponse, renderWithApp } from '../test/render'
import { voiceQuery } from '../api/client'
import History from './History'

afterEach(() => vi.unstubAllGlobals())

const item = {
  id: 7,
  kind: 'CHAT',
  title: 'What is fever?',
  summary: 'Fever is a raised body temperature.',
  language: 'en',
  createdAt: '2026-10-09T06:00:00Z',
}

describe('History page', () => {
  it('is off by default and sends no device id', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ answer: 'ok' }))
    vi.stubGlobal('fetch', fetchMock)
    renderWithApp(<History />)
    expect(await screen.findByText(/History is off/)).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: /Save my scans/ })).not.toBeChecked()

    await voiceQuery('hi', 'en')
    const init = fetchMock.mock.calls[0][1] as RequestInit
    expect(init.headers).not.toHaveProperty('X-Device-Id')
  })

  it('opting in attaches an anonymous device id to requests', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([]))
    vi.stubGlobal('fetch', fetchMock)
    renderWithApp(<History />)
    await userEvent.click(screen.getByRole('checkbox', { name: /Save my scans/ }))
    await voiceQuery('hi', 'en')
    const init = fetchMock.mock.calls.at(-1)?.[1] as RequestInit
    expect((init.headers as Record<string, string>)['X-Device-Id']).toMatch(/^[0-9a-f-]{36}$/)
  })

  it('lists saved items and deletes one', async () => {
    localStorage.setItem('arogyalens.history.enabled', 'true')
    localStorage.setItem('arogyalens.deviceId', '11111111-2222-3333-4444-555555555555')
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse([item]))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(jsonResponse([]))
    vi.stubGlobal('fetch', fetchMock)
    const { container } = renderWithApp(<History />)

    const entry = await screen.findByRole('heading', { name: item.title })
    expect(await axe(container)).toHaveNoViolations()
    await userEvent.click(
      within(entry.closest('li') as HTMLElement).getByRole('button', { name: /Delete/ }),
    )

    expect(fetchMock.mock.calls[1][0]).toBe('/api/history/7')
    expect((fetchMock.mock.calls[1][1] as RequestInit).method).toBe('DELETE')
    expect(await screen.findByText('Nothing saved yet.')).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Deleted.')
  })
})
