import type { DashboardSummaryDto } from '../types'
import { dashboardBucketLabel } from '../lib/statusHelpers'

export function ReportDashboard({ dashboard }: { dashboard: DashboardSummaryDto }) {
  const cards = [
    {
      key: 'within' as const,
      count: dashboard.withinRange,
      tone: 'border-emerald-200 bg-emerald-50/80 text-emerald-950',
    },
    {
      key: 'discuss' as const,
      count: dashboard.needsDiscussion,
      tone: 'border-amber-200 bg-amber-50/80 text-amber-950',
    },
    {
      key: 'attention' as const,
      count: dashboard.importantAttention,
      tone: 'border-red-200 bg-red-50/80 text-red-950',
    },
  ]

  return (
    <section aria-labelledby="dashboard-title">
      <h2 id="dashboard-title" className="font-display text-2xl font-semibold text-brand">
        Report overview
      </h2>
      <p className="text-muted mt-1 max-w-2xl text-sm text-brand/85">
        Counts reflect how results compare to the ranges printed on your document — not a diagnosis.
      </p>
      <div className="mt-5 grid gap-4 sm:grid-cols-3">
        {cards.map((c) => (
          <div key={c.key} className={`rounded-2xl border p-5 ${c.tone}`}>
            <p className="text-3xl font-bold tabular-nums">{c.count}</p>
            <p className="mt-1 text-sm font-semibold">{dashboardBucketLabel(c.key)}</p>
          </div>
        ))}
      </div>
      <p className="text-muted mt-3 text-xs text-brand/85">
        {dashboard.total} parameters detected on this report.
      </p>
    </section>
  )
}
