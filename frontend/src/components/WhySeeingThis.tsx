export function WhySeeingThis() {
  return (
    <section className="rounded-2xl border border-brand/10 bg-surface/80 p-5 text-sm text-brand/85">
      <h2 className="font-display text-lg font-semibold text-brand">Why am I seeing this?</h2>
      <dl className="mt-3 space-y-3">
        <div>
          <dt className="text-xs font-bold uppercase tracking-wide text-brand">
            From your document
          </dt>
          <dd className="mt-1">
            Values, medicine names, and dates taken directly from your upload or demo sample.
          </dd>
        </div>
        <div>
          <dt className="text-xs font-bold uppercase tracking-wide text-brand/85">
            General health information
          </dt>
          <dd className="mt-1">
            Educational context from trusted references — not personalized medical advice.
          </dd>
        </div>
        <div>
          <dt className="text-xs font-bold uppercase tracking-wide text-brand/85">
            AI-generated explanation
          </dt>
          <dd className="mt-1">
            Plain-language summaries produced by ArogyaLens. Always confirm with a qualified
            professional.
          </dd>
        </div>
      </dl>
    </section>
  )
}
