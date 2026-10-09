import { useEffect, useState } from 'react'
import { fetchSpeech } from '../api/client'
import { speak, stopSpeaking } from '../lib/speech'
import type { AppLanguage } from '../types'

interface AudioPlayerProps {
  text: string
  language: AppLanguage
  label?: string
  stopLabel?: string
}

/**
 * Reads text aloud in the given language with the natural Gemini text-to-speech voice,
 * falling back to an on-device voice when the cloud voice is unavailable or slow.
 */
export function AudioPlayer({
  text,
  language,
  label = 'Listen',
  stopLabel = 'Stop',
}: AudioPlayerProps) {
  const [speaking, setSpeaking] = useState(false)
  const [failed, setFailed] = useState(false)
  const [loading, setLoading] = useState(false)

  useEffect(() => () => stopSpeaking(), [])

  const toggle = async () => {
    if (speaking) {
      stopSpeaking()
      setSpeaking(false)
      setLoading(false)
      return
    }
    setFailed(false)
    setSpeaking(true)
    setLoading(true)
    const mode = await speak(text, language, fetchSpeech, () => setSpeaking(false))
    setLoading(false)
    if (mode === 'unsupported') {
      setSpeaking(false)
      setFailed(true)
    }
  }

  return (
    <span className="inline-flex items-center gap-2">
      <button
        type="button"
        onClick={() => void toggle()}
        disabled={!text.trim()}
        aria-pressed={speaking}
        aria-busy={loading}
        className="inline-flex items-center gap-2 rounded-lg border border-brand/30 bg-white px-3 py-2 text-sm font-semibold text-brand transition hover:border-brand/60 disabled:opacity-50"
      >
        <span aria-hidden="true">{speaking ? '■' : '🔊'}</span>
        {speaking ? stopLabel : label}
      </button>
      {loading ? (
        <span role="status" className="text-xs text-brand/85">
          Loading voice…
        </span>
      ) : null}
      {failed ? (
        <span role="status" className="text-xs text-brand/85">
          Audio is not available right now.
        </span>
      ) : null}
    </span>
  )
}
