import { useCallback, useEffect, useRef, useState } from 'react'
import { getRecognitionCtor, speechLocale } from '../lib/speech'
import type { AppLanguage } from '../types'

interface SpeechRecognitionState {
  supported: boolean
  listening: boolean
  error: string | null
  start: () => void
  stop: () => void
}

/**
 * Wraps the browser Web Speech API for speech-to-text in the selected Indian language
 * (en-IN, hi-IN, kn-IN, ta-IN, te-IN, mr-IN, bn-IN). Calls `onResult` with the final transcript.
 */
export function useSpeechRecognition(
  language: AppLanguage,
  onResult: (transcript: string) => void,
): SpeechRecognitionState {
  const [listening, setListening] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const recognitionRef = useRef<AppSpeechRecognition | null>(null)
  const onResultRef = useRef(onResult)
  const supported = getRecognitionCtor() !== null

  useEffect(() => {
    onResultRef.current = onResult
  }, [onResult])

  useEffect(() => () => recognitionRef.current?.abort(), [])

  const start = useCallback(() => {
    const Ctor = getRecognitionCtor()
    if (!Ctor) return
    recognitionRef.current?.abort()
    const rec = new Ctor()
    rec.lang = speechLocale(language)
    rec.interimResults = false
    rec.maxAlternatives = 1
    rec.onresult = (ev) => {
      const transcript = ev.results[0]?.[0]?.transcript ?? ''
      if (transcript) onResultRef.current(transcript)
    }
    rec.onerror = (ev) => {
      setError(
        ev.error === 'not-allowed'
          ? 'Microphone permission was denied.'
          : 'Could not hear that. Please try again.',
      )
      setListening(false)
    }
    rec.onend = () => setListening(false)
    recognitionRef.current = rec
    setError(null)
    setListening(true)
    rec.start()
  }, [language])

  const stop = useCallback(() => {
    recognitionRef.current?.stop()
    setListening(false)
  }, [])

  return { supported, listening, error, start, stop }
}
