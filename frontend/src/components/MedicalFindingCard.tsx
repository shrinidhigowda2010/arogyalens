import { useState } from 'react'
import type { MedicalParameter } from '../types'
import { statusLabel, statusTone } from '../lib/statusHelpers'
import { ExplanationPanel } from './ExplanationPanel'

interface MedicalFindingCardProps {
  parameter: MedicalParameter
  explainLang: import('../types').AppLanguage
  defaultOpen?: boolean
}

export function MedicalFindingCard({
  parameter,
  explainLang,
  defaultOpen = false,
}: MedicalFindingCardProps) {
  const [open, setOpen] = useState(defaultOpen)

  return (
    <article className="overflow-hidden rounded-2xl border border-brand/10 bg-white shadow-sm transition hover:border-brand/20">
      <button
        type="button"
        onClick={() => setOpen(!open)}
        className="flex w-full items-start justify-between gap-4 px-5 py-4 text-left focus:outline-none focus:ring-2 focus:ring-inset focus:ring-brand/30"
        aria-expanded={open}
      >
        <div className="min-w-0 flex-1">
          <h3 className="font-display text-lg font-semibold text-brand">{parameter.name}</h3>
          <p className="mt-1 text-sm text-brand/80">
            <span className="text-lg font-bold text-brand">{parameter.value}</span>
            {parameter.unit ? ` ${parameter.unit}` : ''}
            {parameter.referenceRange ? (
              <span className="text-muted ml-2 text-brand/60">Ref: {parameter.referenceRange}</span>
            ) : null}
          </p>
        </div>
        <span
          className={`shrink-0 rounded-full border px-3 py-1 text-xs font-semibold ${statusTone(parameter.status)}`}
        >
          {statusLabel(parameter.status)}
        </span>
      </button>
      {open ? (
        <div className="border-t border-brand/10 px-5 pb-5 pt-2">
          {parameter.lowConfidence ? (
            <p className="mb-3 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-950">
              This reading had lower confidence from the scan. Confirm the value on your original
              document.
            </p>
          ) : null}
          <ExplanationPanel parameter={parameter} explainLang={explainLang} />
        </div>
      ) : null}
    </article>
  )
}
