import type { PrivacyShieldDto } from '../types'

export function PrivacyShield({ shield }: { shield: PrivacyShieldDto | null }) {
  return (
    <section
      className="animate-fade-up rounded-2xl border border-brand/15 bg-white p-6 shadow-sm"
      aria-labelledby="privacy-shield-title"
    >
      <div className="flex items-start gap-4">
        <div
          className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-brand-muted text-brand"
          aria-hidden
        >
          <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.99 11.99 0 003 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285z"
            />
          </svg>
        </div>
        <div className="min-w-0 flex-1">
          <h2 id="privacy-shield-title" className="font-display text-xl font-semibold text-brand">
            Privacy shield
          </h2>
          <p className="text-muted mt-2 text-sm leading-relaxed text-brand/80">
            {shield?.message ??
              'ArogyaLens detects and masks common personal identifiers before processing.'}
          </p>
          {shield?.findings?.length ? (
            <ul className="mt-4 grid gap-2 sm:grid-cols-2">
              {shield.findings.map((f) => (
                <li
                  key={f.type}
                  className="rounded-lg bg-brand-muted/60 px-3 py-2 text-sm text-brand/90"
                >
                  <span className="font-semibold">{f.type}:</span>{' '}
                  <span className="font-mono text-brand/70">{f.maskedValue}</span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-muted mt-3 text-sm italic text-brand/60">
              Scanning for names, IDs, and contact details…
            </p>
          )}
        </div>
      </div>
    </section>
  )
}
