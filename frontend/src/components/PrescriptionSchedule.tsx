import type { PrescriptionItem } from '../types'

function Slot({ active, label }: { active: boolean; label: string }) {
  return (
    <div
      className={`flex flex-col items-center rounded-xl px-3 py-3 text-center text-xs font-semibold ${
        active ? 'bg-brand text-white shadow-sm' : 'bg-brand-muted/50 text-brand/40'
      }`}
    >
      <span className="text-lg" aria-hidden>
        {active ? '💊' : '·'}
      </span>
      {label}
    </div>
  )
}

export function PrescriptionSchedule({ items }: { items: PrescriptionItem[] }) {
  if (!items?.length) {
    return (
      <p className="text-sm text-brand/70">No prescription items were detected on this scan.</p>
    )
  }

  return (
    <section className="space-y-5" aria-labelledby="rx-schedule">
      <div>
        <h2 id="rx-schedule" className="font-display text-2xl font-semibold text-brand">
          Prescription schedule
        </h2>
        <p className="text-muted mt-1 text-sm text-brand/70">
          Visual guide from your scan — confirm timing with your pharmacist or doctor.
        </p>
      </div>

      {items.map((item) => (
        <article
          key={item.medicineName + item.strength}
          className="rounded-2xl border border-brand/10 bg-white p-5"
        >
          <div className="flex flex-wrap items-start justify-between gap-2">
            <div>
              <h3 className="font-display text-lg font-semibold text-brand">
                {item.medicineName}
                {item.strength ? ` · ${item.strength}` : ''}
              </h3>
              <p className="text-muted mt-1 text-sm text-brand/75">
                {[item.frequency, item.timing, item.foodRelation].filter(Boolean).join(' · ')}
              </p>
            </div>
            {!item.confident ? (
              <span className="rounded-full bg-amber-100 px-3 py-1 text-xs font-bold text-amber-950">
                Unclear — verify on original
              </span>
            ) : null}
          </div>

          <div className="mt-4 grid grid-cols-3 gap-2 sm:max-w-md">
            <Slot active={item.morning} label="Morning" />
            <Slot active={item.afternoon} label="Afternoon" />
            <Slot active={item.night} label="Night" />
          </div>

          {item.note ? (
            <p className="text-muted mt-3 text-sm italic text-brand/70">{item.note}</p>
          ) : null}
        </article>
      ))}
    </section>
  )
}
