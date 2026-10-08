export function DemoModeBanner() {
  return (
    <div
      role="status"
      className="border-b border-amber-200 bg-amber-50 px-4 py-2.5 text-center text-sm text-amber-950"
    >
      <span className="font-semibold">Demo mode</span>
      <span className="text-muted mx-2 text-amber-900/80">—</span>
      Sample data for exploration. Not a real patient record.
    </div>
  )
}
