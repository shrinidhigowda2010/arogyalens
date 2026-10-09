/// <reference types="vite/client" />

/** Minimal typing for the (vendor-prefixed) Web Speech API recognition interface. */
interface AppSpeechRecognitionEvent extends Event {
  readonly results: SpeechRecognitionResultList
}

interface AppSpeechRecognition extends EventTarget {
  lang: string
  interimResults: boolean
  maxAlternatives: number
  start(): void
  stop(): void
  abort(): void
  onresult: ((ev: AppSpeechRecognitionEvent) => void) | null
  onerror: ((ev: Event & { error?: string }) => void) | null
  onend: (() => void) | null
}

type AppSpeechRecognitionConstructor = new () => AppSpeechRecognition

interface Window {
  SpeechRecognition?: AppSpeechRecognitionConstructor
  webkitSpeechRecognition?: AppSpeechRecognitionConstructor
}

interface ImportMetaEnv {
  readonly VITE_API_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
