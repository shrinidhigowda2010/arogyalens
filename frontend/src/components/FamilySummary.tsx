import { useState } from 'react'
import { shareText } from '../lib/share'

export function FamilySummary({ summary }: { summary: string }) {
  const [feedback, setFeedback] = useState<string | null>(null)

  if (!summary?.trim()) return null

  const act = async (mode: 'copy' | 'share') => {
    const result = await shareText('Family summary — ArogyaLens', summary)
    if (mode === 'copy' && result !== 'copied') {
      setFeedback('Could not copy')
    } else {
      setFeedback(
        result === 'shared' ? 'Shared' : result === 'copied' ? 'Copied' : 'Could not share',
      )
    }
    setTimeout(() => setFeedback(null), 2500)
  }

  return (
    <section
      className="rounded-2xl border border-brand/10 bg-white p-6"
      aria-labelledby="family-summary"
    >
      <h2 id="family-summary" className="font-display text-xl font-semibold text-brand">
        Family summary
      </h2>
      <p className="text-muted mt-1 text-sm text-brand/85">
        A plain-language overview you can share with caregivers — not medical advice.
      </p>
      <pre className="mt-4 whitespace-pre-wrap font-sans text-sm leading-relaxed text-brand/90">
        {summary}
      </pre>
      <div className="mt-4 flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => act('copy')}
          className="btn rounded-lg border border-brand/20 px-4 py-2 text-sm font-semibold text-brand"
        >
          Copy
        </button>
        <button
          type="button"
          onClick={() => act('share')}
          className="btn rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white"
        >
          Share
        </button>
        {feedback ? <span className="self-center text-sm text-brand/85">{feedback}</span> : null}
      </div>
    </section>
  )
}
