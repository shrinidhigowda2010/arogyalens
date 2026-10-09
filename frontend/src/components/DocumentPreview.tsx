export function DocumentPreview({ note }: { note: string }) {
  if (!note?.trim()) return null

  return (
    <aside className="rounded-xl border border-dashed border-brand/20 bg-white/60 px-4 py-3 text-sm text-brand/85">
      <span className="font-semibold text-brand">Document preview: </span>
      {note}
    </aside>
  )
}
