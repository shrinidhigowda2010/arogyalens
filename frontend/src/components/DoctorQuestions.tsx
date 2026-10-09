import { useState } from 'react'
import { copyText, shareText } from '../lib/share'

export function DoctorQuestions({ questions }: { questions: string[] }) {
  const [feedback, setFeedback] = useState<string | null>(null)
  const text = questions.map((q, i) => `${i + 1}. ${q}`).join('\n')

  const act = async (mode: 'copy' | 'share') => {
    if (mode === 'copy') {
      const ok = await copyText(text)
      setFeedback(ok ? 'Copied to clipboard' : 'Could not copy')
    } else {
      const result = await shareText('Questions for your doctor', text)
      setFeedback(
        result === 'shared'
          ? 'Shared'
          : result === 'copied'
            ? 'Copied (share unavailable)'
            : 'Could not share',
      )
    }
    setTimeout(() => setFeedback(null), 2500)
  }

  if (!questions.length) return null

  return (
    <section aria-labelledby="doctor-questions-title">
      <h2 id="doctor-questions-title" className="font-display text-xl font-semibold text-brand">
        Questions for your doctor
      </h2>
      <p className="text-muted mt-1 text-sm text-brand/85">
        Bring these to your next appointment. Wording is a starting point — adjust to your
        situation.
      </p>
      <ol className="mt-4 list-decimal space-y-2 pl-5 text-sm text-brand/90">
        {questions.map((q) => (
          <li key={q}>{q}</li>
        ))}
      </ol>
      <div className="mt-4 flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => void act('copy')}
          className="btn rounded-lg border border-brand/20 bg-white px-4 py-2 text-sm font-semibold text-brand hover:border-brand/40"
        >
          Copy
        </button>
        <button
          type="button"
          onClick={() => void act('share')}
          className="btn rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white hover:bg-brand-light"
        >
          Share
        </button>
        {feedback ? <span className="self-center text-sm text-brand/85">{feedback}</span> : null}
      </div>
    </section>
  )
}
