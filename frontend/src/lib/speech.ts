import type { AppLanguage } from '../types'
import { SPEECH_LOCALES } from './constants'

/** BCP-47 locale used for speech recognition and synthesis, e.g. `kn-IN`. */
export function speechLocale(language: AppLanguage): string {
  return SPEECH_LOCALES[language] ?? 'en-IN'
}

/** Returns the browser's speech recognition constructor, if any. */
export function getRecognitionCtor(): AppSpeechRecognitionConstructor | null {
  if (typeof window === 'undefined') return null
  return window.SpeechRecognition ?? window.webkitSpeechRecognition ?? null
}

/** Finds an installed voice for the language (exact locale first, then language prefix). */
export function findVoice(
  language: AppLanguage,
  voices: SpeechSynthesisVoice[],
): SpeechSynthesisVoice | null {
  const locale = speechLocale(language).toLowerCase()
  return (
    voices.find((v) => v.lang.toLowerCase().replace('_', '-') === locale) ??
    voices.find((v) => v.lang.toLowerCase().startsWith(language)) ??
    null
  )
}

const SCRIPT_RANGES: { language: AppLanguage; pattern: RegExp }[] = [
  { language: 'kn', pattern: /[\u0C80-\u0CFF]/ },
  { language: 'ta', pattern: /[\u0B80-\u0BFF]/ },
  { language: 'te', pattern: /[\u0C00-\u0C7F]/ },
  { language: 'bn', pattern: /[\u0980-\u09FF]/ },
  { language: 'hi', pattern: /[\u0900-\u097F]/ },
]

/**
 * Detects the language from the script of the text (Kannada, Tamil, Telugu, Bengali,
 * Devanagari). Devanagari is shared by Hindi and Marathi, so the current choice is kept
 * when it is already one of them. Returns null for Latin text or when unsure.
 */
export function detectLanguage(text: string, current: AppLanguage): AppLanguage | null {
  for (const { language, pattern } of SCRIPT_RANGES) {
    if (pattern.test(text)) {
      if (language === 'hi' && (current === 'hi' || current === 'mr')) return current
      return language
    }
  }
  return null
}

let currentAudio: HTMLAudioElement | null = null

/** Stops any speech that is playing (device voice or cloud audio). */
/** Speaking rate for on-device voices (1 is the browser default). */
export const DEVICE_SPEECH_RATE = 0.85

/** Cloud voice requests slower than this fall back to the device voice. */
export const CLOUD_TIMEOUT_MS = 8000

/** Incremented on every stop/start so a late cloud response never plays after Stop. */
let generation = 0

export function stopSpeaking(): void {
  generation += 1
  if (typeof window !== 'undefined' && 'speechSynthesis' in window) window.speechSynthesis.cancel()
  if (currentAudio) {
    currentAudio.pause()
    currentAudio = null
  }
}

function withTimeout<T>(promise: Promise<T>, ms: number): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('timeout')), ms)
    promise.then(
      (value) => {
        clearTimeout(timer)
        resolve(value)
      },
      (error: unknown) => {
        clearTimeout(timer)
        reject(error instanceof Error ? error : new Error(String(error)))
      },
    )
  })
}

async function playCloud(blob: Blob, onEnd?: () => void): Promise<void> {
  const url = URL.createObjectURL(blob)
  const audio = new Audio(url)
  currentAudio = audio
  const finish = () => {
    URL.revokeObjectURL(url)
    onEnd?.()
  }
  audio.onended = finish
  audio.onerror = finish
  await audio.play()
}

function speakOnDevice(text: string, language: AppLanguage, onEnd?: () => void): boolean {
  const synth =
    typeof window !== 'undefined' && 'speechSynthesis' in window ? window.speechSynthesis : null
  if (!synth) return false
  const voice = findVoice(language, synth.getVoices())
  if (!voice && language !== 'en') return false
  const utter = new SpeechSynthesisUtterance(text)
  utter.lang = speechLocale(language)
  if (voice) utter.voice = voice
  // Slightly slower than normal so older listeners can follow along.
  utter.rate = DEVICE_SPEECH_RATE
  utter.pitch = 1
  utter.onend = () => onEnd?.()
  utter.onerror = () => onEnd?.()
  synth.speak(utter)
  return true
}

/**
 * Reads text aloud. Prefers the natural Gemini text-to-speech voice from the backend and falls
 * back to an on-device voice when the cloud request fails or takes longer than
 * {@link CLOUD_TIMEOUT_MS}.
 *
 * @returns how the text was spoken, 'cancelled' if stopped while loading, or 'unsupported'
 */
export async function speak(
  text: string,
  language: AppLanguage,
  fetchCloudAudio: (text: string, language: AppLanguage) => Promise<Blob>,
  onEnd?: () => void,
): Promise<'device' | 'cloud' | 'cancelled' | 'unsupported'> {
  stopSpeaking()
  if (!text.trim()) return 'unsupported'
  const mine = generation
  try {
    const blob = await withTimeout(fetchCloudAudio(text, language), CLOUD_TIMEOUT_MS)
    if (mine !== generation) return 'cancelled'
    await playCloud(blob, onEnd)
    return 'cloud'
  } catch {
    if (mine !== generation) return 'cancelled'
    currentAudio = null
    if (speakOnDevice(text, language, onEnd)) return 'device'
    onEnd?.()
    return 'unsupported'
  }
}
