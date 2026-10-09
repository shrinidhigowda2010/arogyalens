import { DEFAULT_PROCESSING_LABELS } from '../lib/constants'
import type { ProcessingStepDto } from '../types'

interface ProcessingProgressProps {
  steps: ProcessingStepDto[] | null
  activeIndex: number
  complete: boolean
}

export function ProcessingProgress({ steps, activeIndex, complete }: ProcessingProgressProps) {
  const labels =
    steps?.length && steps.length > 0
      ? steps.map((s) => s.label.replace(/\.\.\.$/, ''))
      : DEFAULT_PROCESSING_LABELS

  return (
    <section
      className="animate-fade-up mt-6 rounded-2xl border border-brand/10 bg-white/90 p-6"
      aria-live="polite"
      aria-busy={!complete}
    >
      <h2 className="font-display text-lg font-semibold text-brand">Preparing your explanation</h2>
      <ol className="mt-5 space-y-3">
        {labels.map((label, i) => {
          const done = complete || i < activeIndex
          const current = !complete && i === activeIndex
          return (
            <li key={label + i} className="flex items-center gap-3 text-sm">
              <span
                className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full border text-xs font-bold ${
                  done
                    ? 'border-brand bg-brand text-white'
                    : current
                      ? 'animate-pulse-soft border-brand/40 bg-brand-muted text-brand'
                      : 'border-brand/15 bg-surface text-brand/40'
                }`}
              >
                {done ? '✓' : i + 1}
              </span>
              <span className={done || current ? 'font-medium text-brand' : 'text-brand/85'}>
                {label}
              </span>
            </li>
          )
        })}
      </ol>
    </section>
  )
}
