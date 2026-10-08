import type { AppLanguage } from '../types'

export const STORAGE_KEYS = {
  language: 'arogyalens_language',
  a11y: 'arogyalens_a11y',
  sessions: 'arogyalens_sessions',
} as const

export const LANGUAGES: { code: AppLanguage; label: string; native: string }[] = [
  { code: 'en', label: 'English', native: 'English' },
  { code: 'hi', label: 'Hindi', native: 'हिन्दी' },
  { code: 'kn', label: 'Kannada', native: 'ಕನ್ನಡ' },
  { code: 'ta', label: 'Tamil', native: 'தமிழ்' },
  { code: 'te', label: 'Telugu', native: 'తెలుగు' },
  { code: 'mr', label: 'Marathi', native: 'मराठी' },
  { code: 'bn', label: 'Bengali', native: 'বাংলা' },
]

export const SPEECH_LOCALES: Record<AppLanguage, string> = {
  en: 'en-IN',
  hi: 'hi-IN',
  kn: 'kn-IN',
  ta: 'ta-IN',
  te: 'te-IN',
  mr: 'mr-IN',
  bn: 'bn-IN',
}

export const DEFAULT_PROCESSING_LABELS = [
  'Protecting personal information',
  'Understanding document',
  'Extracting medical information',
  'Simplifying medical terminology',
  'Preparing language',
  'Finding trusted sources',
  'Safety check',
]
