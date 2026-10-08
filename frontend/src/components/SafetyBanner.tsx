export function SafetyBanner({ notes }: { notes: string[] }) {
  if (!notes?.length) return null

  return (
    <aside
      className="rounded-2xl border border-amber-200 bg-amber-50/90 px-5 py-4"
      role="note"
      aria-label="Safety reminders"
    >
      <h2 className="font-display text-base font-semibold text-amber-950">Safety reminders</h2>
      <ul className="mt-2 space-y-1.5 text-sm text-amber-950/90">
        {notes.map((n) => (
          <li key={n}>{n}</li>
        ))}
      </ul>
    </aside>
  )
}
