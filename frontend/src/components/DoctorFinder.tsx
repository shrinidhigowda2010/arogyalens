import { useId, useState, type FormEvent } from 'react'
import { searchDoctors, suggestSpecialty } from '../api/client'
import { useApp } from '../hooks/useApp'
import { t } from '../lib/i18n'
import type { DoctorSearchResponse, SpecialtySuggestion } from '../types'
import { DoctorResults } from './DoctorResults'
import { EmergencyBanner } from './EmergencyBanner'

interface DoctorFinderProps {
  /** Pre-filled concern, e.g. out-of-range results from a scanned report. */
  initialConcern?: string
}

interface Coords {
  latitude: number
  longitude: number
}

/**
 * Doctor consultation bar: concern → suggested specialty → nearby doctors.
 * Shows only real places from Google Places (when configured on the server) or deep links
 * to Google Maps, Practo and eSanjeevani. It never invents doctors or phone numbers.
 */
export function DoctorFinder({ initialConcern = '' }: DoctorFinderProps) {
  const { language } = useApp()
  const ids = { concern: useId(), specialty: useId(), location: useId() }
  const [concern, setConcern] = useState(initialConcern)
  const [suggestion, setSuggestion] = useState<SpecialtySuggestion | null>(null)
  const [specialty, setSpecialty] = useState('General Physician')
  const [location, setLocation] = useState('')
  const [coords, setCoords] = useState<Coords | null>(null)
  const [results, setResults] = useState<DoctorSearchResponse | null>(null)
  const [busy, setBusy] = useState<'suggest' | 'locate' | 'search' | null>(null)
  const [error, setError] = useState<string | null>(null)

  const onSuggest = async (e: FormEvent) => {
    e.preventDefault()
    if (!concern.trim()) return
    setBusy('suggest')
    setError(null)
    try {
      const s = await suggestSpecialty(concern.trim(), language)
      setSuggestion(s)
      setSpecialty(s.specialty)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not suggest a specialist.')
    } finally {
      setBusy(null)
    }
  }

  const locate = () => {
    if (!('geolocation' in navigator)) {
      setError('Location is not available in this browser. Please type your city or PIN code.')
      return
    }
    setBusy('locate')
    setError(null)
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setCoords({ latitude: pos.coords.latitude, longitude: pos.coords.longitude })
        setBusy(null)
      },
      () => {
        setError('Location permission was denied. Please type your city or PIN code.')
        setBusy(null)
      },
      { enableHighAccuracy: false, timeout: 10_000, maximumAge: 300_000 },
    )
  }

  const onSearch = async (e: FormEvent) => {
    e.preventDefault()
    if (!specialty.trim() || (!coords && !location.trim())) {
      setError('Please allow location access or type your city or PIN code.')
      return
    }
    setBusy('search')
    setError(null)
    try {
      setResults(
        await searchDoctors({
          specialty: specialty.trim(),
          location: location.trim() || undefined,
          latitude: location.trim() ? undefined : coords?.latitude,
          longitude: location.trim() ? undefined : coords?.longitude,
          language,
        }),
      )
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not search for doctors.')
    } finally {
      setBusy(null)
    }
  }

  const inputClass =
    'w-full rounded-xl border border-brand/30 bg-white px-4 py-3 text-base text-brand placeholder:text-brand/85'

  return (
    <section
      aria-labelledby={`${ids.concern}-title`}
      className="space-y-5 rounded-2xl border border-brand/15 bg-white p-5"
    >
      <div>
        <h2 id={`${ids.concern}-title`} className="font-display text-xl font-semibold text-brand">
          {t(language, 'doctors')}
        </h2>
        <p className="mt-1 text-sm text-brand/85">{t(language, 'doctorsDesc')}</p>
      </div>

      <form onSubmit={onSuggest} className="space-y-2">
        <label htmlFor={ids.concern} className="block text-sm font-semibold text-brand">
          {t(language, 'concernLabel')}
        </label>
        <div className="flex flex-col gap-2 sm:flex-row">
          <input
            id={ids.concern}
            lang={language}
            value={concern}
            maxLength={500}
            onChange={(e) => setConcern(e.target.value)}
            placeholder={t(language, 'concernPlaceholder')}
            className={inputClass}
          />
          <button
            type="submit"
            disabled={!concern.trim() || busy !== null}
            className="btn shrink-0 rounded-xl bg-brand-muted px-4 py-3 text-sm font-semibold text-brand disabled:opacity-60"
          >
            {busy === 'suggest' ? t(language, 'searching') : t(language, 'suggestSpecialty')}
          </button>
        </div>
      </form>

      <div aria-live="polite">
        {suggestion?.urgent ? <EmergencyBanner /> : null}
        {suggestion && !suggestion.urgent ? (
          <p className="rounded-xl bg-brand-muted/60 p-3 text-sm text-brand">
            <strong>{suggestion.specialty}</strong>
            {suggestion.reason ? ` — ${suggestion.reason}` : ''}
          </p>
        ) : null}
      </div>

      <form onSubmit={onSearch} className="grid gap-3 sm:grid-cols-2">
        <div>
          <label htmlFor={ids.specialty} className="block text-sm font-semibold text-brand">
            {t(language, 'specialtyLabel')}
          </label>
          <input
            id={ids.specialty}
            value={specialty}
            maxLength={60}
            onChange={(e) => setSpecialty(e.target.value)}
            className={`mt-1 ${inputClass}`}
          />
        </div>
        <div>
          <label htmlFor={ids.location} className="block text-sm font-semibold text-brand">
            {t(language, 'locationLabel')}
          </label>
          <input
            id={ids.location}
            value={location}
            maxLength={100}
            autoComplete="address-level2"
            onChange={(e) => setLocation(e.target.value)}
            placeholder={coords ? 'Using your current location' : 'e.g. Mysuru or 570001'}
            className={`mt-1 ${inputClass}`}
          />
        </div>
        <div className="flex flex-wrap gap-2 sm:col-span-2">
          <button
            type="button"
            onClick={locate}
            disabled={busy !== null}
            aria-pressed={coords !== null}
            className="btn rounded-xl border border-brand/30 bg-white px-4 py-3 text-sm font-semibold text-brand disabled:opacity-60"
          >
            <span aria-hidden="true">📍 </span>
            {busy === 'locate' ? t(language, 'locating') : t(language, 'useMyLocation')}
          </button>
          <button
            type="submit"
            disabled={busy !== null}
            className="btn rounded-xl bg-brand px-5 py-3 text-sm font-semibold text-white disabled:opacity-60"
          >
            {busy === 'search' ? t(language, 'searching') : t(language, 'findDoctors')}
          </button>
        </div>
      </form>

      {error ? (
        <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800">
          {error}
        </p>
      ) : null}

      <div aria-live="polite" className="space-y-3">
        {results ? <DoctorResults results={results} /> : null}
      </div>
    </section>
  )
}
