import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { axe } from '../test/axe'
import { jsonResponse, renderWithApp } from '../test/render'
import { DoctorFinder } from './DoctorFinder'

afterEach(() => vi.unstubAllGlobals())

const links = [
  {
    label: 'Google Maps',
    url: 'https://www.google.com/maps/search/?api=1&query=x',
    description: 'Maps',
  },
  {
    label: 'eSanjeevani (Govt. of India)',
    url: 'https://esanjeevani.mohfw.gov.in/',
    description: 'Tele',
  },
]

describe('DoctorFinder', () => {
  it('suggests a specialty and shows deep links when live listings are off', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse({
          specialty: 'Cardiologist',
          reason: 'Heart specialist.',
          urgent: false,
          source: 'rules',
        }),
      )
      .mockResolvedValueOnce(
        jsonResponse({
          specialty: 'Cardiologist',
          locationLabel: 'Mysuru',
          placesEnabled: false,
          places: [],
          links,
          notice: 'Live listings are not enabled on this server.',
        }),
      )
    vi.stubGlobal('fetch', fetchMock)
    renderWithApp(<DoctorFinder />)

    await userEvent.type(
      screen.getByLabelText('Symptoms, condition or test result'),
      'palpitations',
    )
    await userEvent.click(screen.getByRole('button', { name: 'Suggest specialist' }))
    expect(await screen.findByLabelText('Specialist to look for')).toHaveValue('Cardiologist')

    await userEvent.type(screen.getByLabelText('City or PIN code'), 'Mysuru')
    await userEvent.click(screen.getByRole('button', { name: 'Find doctors' }))

    expect(await screen.findByRole('link', { name: /Google Maps/ })).toHaveAttribute(
      'target',
      '_blank',
    )
    expect(screen.queryByRole('link', { name: /^Call/ })).not.toBeInTheDocument()
    const body = JSON.parse(
      (fetchMock.mock.calls[1] as [string, RequestInit])[1].body as string,
    ) as Record<string, unknown>
    expect(body).toMatchObject({ specialty: 'Cardiologist', location: 'Mysuru', language: 'en' })
  })

  it('renders real places with call and directions actions', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse({
          specialty: 'Dentist',
          locationLabel: 'Pune',
          placesEnabled: true,
          places: [
            {
              id: 'p1',
              name: 'Smile Dental',
              address: 'FC Road',
              rating: 4.6,
              ratingCount: 210,
              phone: '020 1234 5678',
              openNow: true,
              mapsUrl: 'https://maps.google.com/?cid=1',
            },
          ],
          links,
        }),
      ),
    )
    renderWithApp(<DoctorFinder />)
    const specialty = screen.getByLabelText('Specialist to look for')
    await userEvent.clear(specialty)
    await userEvent.type(specialty, 'Dentist')
    await userEvent.type(screen.getByLabelText('City or PIN code'), 'Pune')
    await userEvent.click(screen.getByRole('button', { name: 'Find doctors' }))

    expect(await screen.findByRole('heading', { name: 'Smile Dental' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Call Smile Dental' })).toHaveAttribute(
      'href',
      'tel:02012345678',
    )
    expect(screen.getByText(/Open now/)).toBeInTheDocument()
  })

  it('shows the emergency banner for urgent concerns', async () => {
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValue(
          jsonResponse({ specialty: 'Cardiologist', reason: '', urgent: true, source: 'rules' }),
        ),
    )
    renderWithApp(<DoctorFinder initialConcern="crushing chest pain" />)
    await userEvent.click(screen.getByRole('button', { name: 'Suggest specialist' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('112')
  })

  it('asks for a location before searching', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    renderWithApp(<DoctorFinder />)
    await userEvent.click(screen.getByRole('button', { name: 'Find doctors' }))
    expect(await screen.findByRole('alert')).toHaveTextContent(/city or PIN code/)
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('has no detectable accessibility violations', async () => {
    const { container } = renderWithApp(<DoctorFinder />)
    expect(await axe(container)).toHaveNoViolations()
  })
})
