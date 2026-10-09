import type { DischargeSummary } from '../types'
import { DoctorQuestions } from './DoctorQuestions'

export function DischargeSections({ summary }: { summary: DischargeSummary }) {
  const blocks: { title: string; items?: string[]; body?: string }[] = [
    { title: 'Why admitted', body: summary.reasonForAdmission },
    { title: 'Treatment performed', body: summary.treatmentPerformed },
    { title: 'Important findings', items: summary.importantFindings },
    { title: 'Medicines listed', items: summary.medicinesListed },
    { title: 'Follow-up', items: summary.followUpInstructions },
    { title: 'Warning signs', items: summary.warningSigns },
  ]

  return (
    <section className="space-y-6" aria-labelledby="discharge-title">
      <h2 id="discharge-title" className="font-display text-2xl font-semibold text-brand">
        Discharge summary
      </h2>
      <p className="text-muted text-sm text-brand/85">
        Section-by-section view of your discharge document — for understanding, not treatment
        decisions.
      </p>

      {blocks.map((b) => (
        <article key={b.title} className="rounded-2xl border border-brand/10 bg-white p-5">
          <h3 className="font-display text-lg font-semibold text-brand">{b.title}</h3>
          {b.body ? <p className="mt-2 text-sm leading-relaxed text-brand/85">{b.body}</p> : null}
          {b.items?.length ? (
            <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-brand/85">
              {b.items.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          ) : null}
        </article>
      ))}

      {summary.doctorQuestions?.length ? (
        <DoctorQuestions questions={summary.doctorQuestions} />
      ) : null}
    </section>
  )
}
