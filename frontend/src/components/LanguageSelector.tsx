import { useId } from 'react'
import { useApp } from '../hooks/useApp'
import { LANGUAGES } from '../lib/constants'
import type { AppLanguage } from '../types'

interface LanguageSelectorProps {
  /** Visible label; hidden visually (but kept for screen readers) when `compact`. */
  label?: string
  compact?: boolean
}

/** Picker for the app-wide language, which also drives speech and AI answer language. */
export function LanguageSelector({ label = 'Language', compact }: LanguageSelectorProps) {
  const { language, setLanguage } = useApp()
  const id = useId()

  return (
    <div className={`flex items-center gap-2 ${compact ? 'text-sm' : ''}`}>
      <label htmlFor={id} className={compact ? 'sr-only' : 'text-sm font-medium text-brand/85'}>
        {label}
      </label>
      <select
        id={id}
        value={language}
        onChange={(e) => setLanguage(e.target.value as AppLanguage)}
        className="btn rounded-lg border border-brand/30 bg-white px-3 py-2 text-sm font-medium text-brand shadow-sm transition hover:border-brand/50"
      >
        {LANGUAGES.map((l) => (
          <option key={l.code} value={l.code} lang={l.code}>
            {l.native} ({l.label})
          </option>
        ))}
      </select>
    </div>
  )
}
