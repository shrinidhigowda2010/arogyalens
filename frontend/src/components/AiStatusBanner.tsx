import { useEffect, useState } from 'react'
import { checkHealth } from '../api/client'

export function AiStatusBanner() {
  const [aiConfigured, setAiConfigured] = useState<boolean | null>(null)

  useEffect(() => {
    let cancelled = false
    checkHealth()
      .then((h) => {
        if (!cancelled) setAiConfigured(Boolean(h.aiConfigured))
      })
      .catch(() => {
        if (!cancelled) setAiConfigured(null)
      })
    return () => {
      cancelled = true
    }
  }, [])

  if (aiConfigured !== false) return null

  return (
    <div className="border-b border-amber-200 bg-amber-50 px-4 py-2 text-center text-sm text-amber-950">
      Image recognition needs a Gemini API key. Add <code className="font-mono">GEMINI_API_KEY</code> to
      the project <code className="font-mono">.env</code> and restart the backend. Text PDFs still work.
    </div>
  )
}
