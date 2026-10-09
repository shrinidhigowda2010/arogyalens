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
    <div
      role="status"
      className="border-b border-amber-200 bg-amber-50 px-4 py-2 text-center text-sm text-amber-950"
    >
      AI image reading is not configured on this server. Text-based PDF reports still work.
    </div>
  )
}
