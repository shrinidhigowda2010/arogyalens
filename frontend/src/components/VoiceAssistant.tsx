import { useCallback, useEffect, useRef, useState } from 'react'
import { voiceQuery } from '../api/client'
import { useApp } from '../context/AppContext'

interface VoiceAssistantProps {
  sessionId: string
}

export function VoiceAssistant({ sessionId }: VoiceAssistantProps) {
  const { language, accessibilityMode } = useApp()
  const [text, setText] = useState('')
  const [answer, setAnswer] = useState<string | null>(null)
  const [listening, setListening] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const recognitionRef = useRef<SpeechRecognition | null>(null)

  const speechSupported =
    typeof window !== 'undefined' &&
    ('SpeechRecognition' in window || 'webkitSpeechRecognition' in window)

  useEffect(() => {
    if (!speechSupported) return
    const Ctor =
      window.SpeechRecognition ??
      (window as unknown as { webkitSpeechRecognition: typeof SpeechRecognition })
        .webkitSpeechRecognition
    const rec = new Ctor()
    rec.lang = language === 'en' ? 'en-IN' : `${language}-IN`
    rec.interimResults = false
    rec.maxAlternatives = 1
    rec.onresult = (ev) => {
      const transcript = ev.results[0]?.[0]?.transcript ?? ''
      setText(transcript)
      setListening(false)
    }
    rec.onerror = () => setListening(false)
    rec.onend = () => setListening(false)
    recognitionRef.current = rec
  }, [language, speechSupported])

  const toggleListen = () => {
    if (!recognitionRef.current) return
    if (listening) {
      recognitionRef.current.stop()
      setListening(false)
    } else {
      setError(null)
      recognitionRef.current.start()
      setListening(true)
    }
  }

  const submit = useCallback(async () => {
    const q = text.trim()
    if (!q || loading) return
    setLoading(true)
    setError(null)
    try {
      const res = await voiceQuery(q, sessionId, language)
      setAnswer(res.answer)
      if (accessibilityMode && 'speechSynthesis' in window) {
        const utter = new SpeechSynthesisUtterance(res.answer)
        utter.lang = language === 'en' ? 'en-IN' : `${language}-IN`
        window.speechSynthesis.speak(utter)
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Voice query failed.')
    } finally {
      setLoading(false)
    }
  }, [text, loading, sessionId, language, accessibilityMode])

  return (
    <section className="rounded-2xl border border-brand/10 bg-white p-6" aria-labelledby="voice-title">
      <h2 id="voice-title" className="font-display text-xl font-semibold text-brand">
        Voice assistant
      </h2>
      <p className="text-muted mt-1 text-sm text-brand/70">
        {speechSupported
          ? 'Speak or type a question about this session.'
          : 'Speech recognition is unavailable — use text input.'}
      </p>

      <div className="mt-4 flex flex-wrap gap-2">
        {speechSupported ? (
          <button
            type="button"
            onClick={toggleListen}
            className={`btn rounded-xl px-4 py-2.5 text-sm font-semibold ${
              listening ? 'bg-red-600 text-white' : 'bg-brand-muted text-brand'
            }`}
          >
            {listening ? 'Stop listening' : 'Start voice'}
          </button>
        ) : null}
      </div>

      <form
        className="mt-3 flex flex-col gap-2 sm:flex-row"
        onSubmit={(e) => {
          e.preventDefault()
          void submit()
        }}
      >
        <input
          value={text}
          onChange={(e) => setText(e.target.value)}
          className="flex-1 rounded-xl border border-brand/20 px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand/30"
          placeholder="e.g. What is HbA1c on my report?"
        />
        <button
          type="submit"
          disabled={loading}
          className="btn rounded-xl bg-brand px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-50"
        >
          {loading ? 'Asking…' : 'Ask'}
        </button>
      </form>

      {error ? (
        <p className="mt-2 text-sm text-red-700" role="alert">
          {error}
        </p>
      ) : null}
      {answer ? (
        <div className="mt-4 rounded-xl bg-brand-muted/50 p-4 text-sm leading-relaxed text-brand/90">
          {answer}
        </div>
      ) : null}
    </section>
  )
}
