import { render, type RenderResult } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { AppProvider } from '../context/AppContext'

/** Renders UI inside the app providers and an in-memory router. */
export function renderWithApp(ui: ReactElement, route = '/'): RenderResult {
  return render(
    <AppProvider>
      <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
    </AppProvider>,
  )
}

/** Creates a JSON Response for fetch mocks. */
export function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}
