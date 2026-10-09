import { useId, useState } from 'react'
import { statusLabel, statusTone } from '../lib/statusHelpers'
import type { AppLanguage, MedicalParameter } from '../types'
import { ExplanationPanel } from './ExplanationPanel'

interface MedicalFindingCardProps {
  parameter: MedicalParameter
  explainLang: AppLanguage
  defaultOpen?: boolean
}

/** Expandable (accordion) card for one lab parameter. */
export function MedicalFindingCard({
  parameter,
  explainLang,
  defaultOpen = false,
}: MedicalFindingCardProps) {
  const [open, setOpen] = useState(defaultOpen)
  const panelId = useId()

  return (
    <article className="overflow-hidden rounded-2xl border border-brand/15 bg-white shadow-sm transition hover:border-brand/30">
      <h3 className="m-0">
        <button
          type="button"
          onClick={() => setOpen(!open)}
          className="flex w-full items-start justify-between gap-4 px-5 py-4 text-left"
          aria-expanded={open}
          aria-controls={panelId}
        >
          <span className="min-w-0 flex-1">
            <span className="font-display block text-lg font-semibold text-brand">
              {parameter.name}
            </span>
            <span className="mt-1 block text-sm text-brand/85">
              <span className="text-lg font-bold text-brand">{parameter.value}</span>
              {parameter.unit ? ` ${parameter.unit}` : ''}
              {parameter.referenceRange ? (
                <span className="ml-2">Ref: {parameter.referenceRange}</span>
              ) : null}
            </span>
          </span>
          <span
            className={`shrink-0 rounded-full border px-3 py-1 text-xs font-semibold ${statusTone(parameter.status)}`}
          >
            {statusLabel(parameter.status)}
          </span>
        </button>
      </h3>
      {open ? (
        <div id={panelId} className="border-t border-brand/10 px-5 pb-5 pt-2">
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
