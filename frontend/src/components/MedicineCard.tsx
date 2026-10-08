import type { MedicineInfo } from '../types'
import { SourceCard } from './SourceCard'

export function MedicineCard({ medicine }: { medicine: MedicineInfo }) {
  return (
    <section className="space-y-6" aria-labelledby="medicine-title">
      <div>
        <h2 id="medicine-title" className="font-display text-2xl font-semibold text-brand">
          {medicine.name}
          {medicine.strength ? (
            <span className="text-brand/70"> · {medicine.strength}</span>
          ) : null}
        </h2>
        <p className="text-muted mt-1 text-sm text-brand/70">
          {medicine.dosageForm}
          {medicine.manufacturer ? ` · ${medicine.manufacturer}` : ''}
        </p>
        <p className="mt-3 rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-950">
          General information only — not personal prescribing advice. Follow your clinician&apos;s
          instructions for your care.
        </p>
      </div>

      <div className="rounded-2xl border border-brand/10 bg-white p-5">
        <h3 className="font-display text-lg font-semibold text-brand">General use</h3>
        <p className="mt-2 text-sm leading-relaxed text-brand/85">{medicine.generalUse}</p>
      </div>

      {medicine.commonSideEffects?.length ? (
        <div>
          <h3 className="font-display text-lg font-semibold text-brand">Commonly reported effects</h3>
          <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-brand/85">
            {medicine.commonSideEffects.map((s) => (
              <li key={s}>{s}</li>
            ))}
          </ul>
        </div>
      ) : null}

      {medicine.precautions?.length ? (
        <div>
          <h3 className="font-display text-lg font-semibold text-brand">Precautions</h3>
          <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-brand/85">
            {medicine.precautions.map((s) => (
              <li key={s}>{s}</li>
            ))}
          </ul>
        </div>
      ) : null}

      {medicine.warnings?.length ? (
        <div className="rounded-xl border border-red-200 bg-red-50/80 p-4">
          <h3 className="font-semibold text-red-950">Warnings</h3>
          <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-red-950/90">
            {medicine.warnings.map((s) => (
              <li key={s}>{s}</li>
            ))}
          </ul>
        </div>
      ) : null}

      {medicine.sources?.length ? (
        <div>
          <h3 className="font-display text-lg font-semibold text-brand">Trusted sources</h3>
          <div className="mt-3 grid gap-3 sm:grid-cols-2">
            {medicine.sources.map((s) => (
              <SourceCard key={s.id} source={s} />
            ))}
          </div>
        </div>
      ) : null}
    </section>
  )
}
