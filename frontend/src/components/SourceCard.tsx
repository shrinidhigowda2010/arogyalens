import type { TrustedSource } from '../types'

export function SourceCard({ source }: { source: TrustedSource }) {
  return (
    <article className="rounded-xl border border-brand/10 bg-white p-4 transition hover:border-brand/25">
      <p className="text-xs font-semibold uppercase tracking-wide text-brand/85">
        {source.category}
      </p>
      <h3 className="mt-1 font-display text-base font-semibold text-brand">{source.title}</h3>
      <p className="text-muted mt-2 text-sm text-brand/85">{source.description}</p>
      <p className="mt-2 text-xs font-medium text-brand/85">{source.name}</p>
      {source.url ? (
        <a
          href={source.url}
          target="_blank"
          rel="noopener noreferrer"
          className="mt-3 inline-block text-sm font-semibold text-brand underline decoration-brand/30 underline-offset-2 hover:decoration-brand"
        >
          Open source
        </a>
      ) : null}
    </article>
  )
}
