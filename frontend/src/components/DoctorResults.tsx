import { useApp } from '../hooks/useApp'
import { t } from '../lib/i18n'
import type { DoctorSearchResponse } from '../types'

/** Real places from Google Places (when configured) plus deep links; never fabricated doctors. */
export function DoctorResults({ results }: { results: DoctorSearchResponse }) {
  const { language } = useApp()
  return (
    <div className="space-y-3">
      <h3 className="text-base font-semibold text-brand">
        {results.specialty} · {results.locationLabel}
      </h3>
      {results.notice ? <p className="text-sm text-brand/85">{results.notice}</p> : null}
      {results.places.length ? (
        <ul className="space-y-2">
          {results.places.map((p) => (
            <li key={p.id ?? p.name} className="rounded-xl border border-brand/15 p-3">
              <h4 className="font-semibold text-brand">{p.name}</h4>
              {p.address ? <p className="text-sm text-brand/85">{p.address}</p> : null}
              <p className="mt-1 text-sm text-brand/85">
                {p.rating != null ? `★ ${p.rating.toFixed(1)} (${p.ratingCount ?? 0}) · ` : ''}
                {p.openNow == null
                  ? ''
                  : p.openNow
                    ? t(language, 'openNow')
                    : t(language, 'closedNow')}
              </p>
              <div className="mt-2 flex flex-wrap gap-2">
                {p.phone ? (
                  <a
                    href={`tel:${p.phone.replace(/[^+\d]/g, '')}`}
                    className="btn rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white"
                  >
                    {t(language, 'call')}
                    <span className="sr-only"> {p.name}</span>
                  </a>
                ) : null}
                {p.mapsUrl ? (
                  <a
                    href={p.mapsUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="btn rounded-lg border border-brand/30 px-3 py-2 text-sm font-semibold text-brand"
                  >
                    {t(language, 'directions')}
                    <span className="sr-only"> to {p.name} (opens in a new tab)</span>
                  </a>
                ) : null}
              </div>
            </li>
          ))}
        </ul>
      ) : null}
      <ul className="grid gap-2 sm:grid-cols-3">
        {results.links.map((l) => (
          <li key={l.label}>
            <a
              href={l.url}
              target="_blank"
              rel="noopener noreferrer"
              className="block h-full rounded-xl border border-brand/20 bg-surface p-3 hover:border-brand/50"
            >
              <span className="font-semibold text-brand">{l.label}</span>
              <span className="sr-only"> (opens in a new tab)</span>
              <span className="mt-1 block text-xs text-brand/85">{l.description}</span>
            </a>
          </li>
        ))}
      </ul>
    </div>
  )
}
