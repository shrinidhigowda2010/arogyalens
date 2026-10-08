import { useCallback, useEffect, useRef, useState } from 'react'
import { SPEECH_LOCALES } from '../lib/constants'
import type { AppLanguage } from '../types'

interface AudioPlayerProps {
  text: string
  language: AppLanguage
  label?: string
}

export function AudioPlayer({ text, language, label = 'Listen' }: AudioPlayerProps) {
  const [speaking, setSpeaking] = useState(false)
  const utterRef = useRef<SpeechSynthesisUtterance | null>(null)

  const stop = useCallback(() => {
    window.speechSynthesis.cancel()
    setSpeaking(false)
  }, [])

  useEffect(() => () => stop(), [stop])

  const speak = () => {
    if (!('speechSynthesis' in window) || !text.trim()) return
    stop()
    const utter = new SpeechSynthesisUtterance(text)
    utter.lang = SPEECH_LOCALES[language] ?? 'en-IN'
    const voices = window.speechSynthesis.getVoices()
    const match = voices.find((v) => v.lang.startsWith(language))
    if (match) utter.voice = match
    utter.onend = () => setSpeaking(false)
    utter.onerror = () => setSpeaking(false)
    utterRef.current = utter
    window.speechSynthesis.speak(utter)
    setSpeaking(true)
  }

  const supported = typeof window !== 'undefined' && 'speechSynthesis' in window

  if (!supported) {
    return (
      <p className="text-muted text-xs text-brand/60">Listen is not supported in this browser.</p>
    )
  }

  return (
    <button
      type="button"
      onClick={speaking ? stop : speak}
      className="inline-flex items-center gap-2 rounded-lg border border-brand/20 bg-white px-3 py-2 text-sm font-semibold text-brand transition hover:border-brand/40 focus:outline-none focus:ring-2 focus:ring-brand/30"
    >
      {speaking ? 'Stop' : label}
    </button>
  )
}
