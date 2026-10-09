import { useCallback, useEffect, useRef, useState } from 'react'
import { transcribeAudio } from '../api/client'
import { getRecognitionCtor, speechLocale } from '../lib/speech'
import type { AppLanguage } from '../types'

interface SpeechRecognitionState {
  supported: boolean
  listening: boolean
  error: string | null
  start: () => void
  stop: () => void
}

/** Longest recording sent for transcription when the browser has no speech recognition. */
export const MAX_RECORDING_MS = 15000

/** True when the browser can record audio (MediaRecorder + getUserMedia). */
export function canRecordAudio(): boolean {
  return (
    typeof window !== 'undefined' &&
    'MediaRecorder' in window &&
    typeof navigator !== 'undefined' &&
    typeof navigator.mediaDevices?.getUserMedia === 'function'
  )
}

/**
 * Wraps the browser Web Speech API for speech-to-text in the selected Indian language
 * (en-IN, hi-IN, kn-IN, ta-IN, te-IN, mr-IN, bn-IN). Calls `onResult` with the final transcript.
 * Browsers without speech recognition (e.g. Firefox) record up to {@link MAX_RECORDING_MS} of
 * audio instead and have Gemini transcribe it on the backend.
 */
export function useSpeechRecognition(
  language: AppLanguage,
  onResult: (transcript: string) => void,
): SpeechRecognitionState {
  const [listening, setListening] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const recognitionRef = useRef<AppSpeechRecognition | null>(null)
  const onResultRef = useRef(onResult)
  const recorderRef = useRef<MediaRecorder | null>(null)
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const supported = getRecognitionCtor() !== null || canRecordAudio()

  useEffect(() => {
    onResultRef.current = onResult
  }, [onResult])

  useEffect(
    () => () => {
      recognitionRef.current?.abort()
      if (timerRef.current) clearTimeout(timerRef.current)
      if (recorderRef.current?.state === 'recording') recorderRef.current.stop()
    },
    [],
  )

  const record = useCallback(async () => {
    setError(null)
    let stream: MediaStream
    try {
      stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    } catch {
      setError('Microphone permission was denied.')
      return
    }
    const recorder = new MediaRecorder(stream)
    const chunks: Blob[] = []
    recorder.ondataavailable = (ev) => {
      if (ev.data.size > 0) chunks.push(ev.data)
    }
    recorder.onstop = () => {
      if (timerRef.current) clearTimeout(timerRef.current)
      stream.getTracks().forEach((track) => track.stop())
      setListening(false)
      const audio = new Blob(chunks, { type: recorder.mimeType || 'audio/webm' })
      if (audio.size === 0) return
      transcribeAudio(audio, language).then(
        (text) => {
          if (text.trim()) onResultRef.current(text.trim())
          else setError('Could not hear that. Please try again.')
        },
        () => setError('Could not hear that. Please try again or type.'),
      )
    }
    recorderRef.current = recorder
    recorder.start()
    setListening(true)
    timerRef.current = setTimeout(() => {
      if (recorder.state === 'recording') recorder.stop()
    }, MAX_RECORDING_MS)
  }, [language])

  const start = useCallback(() => {
    const Ctor = getRecognitionCtor()
    if (!Ctor) {
      if (canRecordAudio()) void record()
      return
    }
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
  }, [language, record])

  const stop = useCallback(() => {
    recognitionRef.current?.stop()
    if (recorderRef.current?.state === 'recording') recorderRef.current.stop()
    setListening(false)
  }, [])

  return { supported, listening, error, start, stop }
}
