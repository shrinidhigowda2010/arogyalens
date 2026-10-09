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
export function stopSpeaking(): void {
  if (typeof window !== 'undefined' && 'speechSynthesis' in window) window.speechSynthesis.cancel()
  if (currentAudio) {
    currentAudio.pause()
    currentAudio = null
  }
}

/**
 * Reads text aloud. Uses an on-device voice for the language when available, otherwise
 * falls back to Gemini text-to-speech served by the backend.
 *
 * @returns how the text was spoken, or 'unsupported'
 */
export async function speak(
  text: string,
  language: AppLanguage,
  fetchCloudAudio: (text: string, language: AppLanguage) => Promise<Blob>,
  onEnd?: () => void,
): Promise<'device' | 'cloud' | 'unsupported'> {
  stopSpeaking()
  if (!text.trim()) return 'unsupported'
  const synth =
    typeof window !== 'undefined' && 'speechSynthesis' in window ? window.speechSynthesis : null
  const voice = synth ? findVoice(language, synth.getVoices()) : null
  if (synth && (voice || language === 'en')) {
    const utter = new SpeechSynthesisUtterance(text)
    utter.lang = speechLocale(language)
    if (voice) utter.voice = voice
    utter.onend = () => onEnd?.()
    utter.onerror = () => onEnd?.()
    synth.speak(utter)
    return 'device'
  }
  try {
    const blob = await fetchCloudAudio(text, language)
    const url = URL.createObjectURL(blob)
    const audio = new Audio(url)
    currentAudio = audio
    audio.onended = () => {
      URL.revokeObjectURL(url)
      onEnd?.()
    }
    await audio.play()
    return 'cloud'
  } catch {
    onEnd?.()
    return 'unsupported'
  }
}
