import { LANGUAGES } from '../lib/constants'
import { useApp } from '../context/AppContext'
import type { AppLanguage } from '../types'

export function LanguageSelector({ compact }: { compact?: boolean }) {
  const { language, setLanguage } = useApp()

  return (
    <label className={`flex items-center gap-2 ${compact ? 'text-sm' : ''}`}>
      <span className="sr-only">Choose language</span>
      {!compact && (
        <span className="text-muted hidden text-sm font-medium text-brand/80 sm:inline">Language</span>
      )}
      <select
        value={language}
        onChange={(e) => setLanguage(e.target.value as AppLanguage)}
        className="btn rounded-lg border border-brand/20 bg-white px-3 py-2 text-sm font-medium text-brand shadow-sm transition hover:border-brand/40 focus:outline-none focus:ring-2 focus:ring-brand/30"
        aria-label="Language"
      >
        {LANGUAGES.map((l) => (
          <option key={l.code} value={l.code}>
            {l.native}
          </option>
        ))}
      </select>
    </label>
  )
}
