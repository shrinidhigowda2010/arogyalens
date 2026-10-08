import { LANGUAGES } from '../lib/constants'
import type { AppLanguage } from '../types'

interface LanguageSwitcherProps {
  value: AppLanguage
  onChange: (lang: AppLanguage) => void
  label?: string
}

export function LanguageSwitcher({ value, onChange, label = 'Explanation language' }: LanguageSwitcherProps) {
  return (
    <div role="tablist" aria-label={label} className="flex flex-wrap gap-2">
      {LANGUAGES.map((l) => (
        <button
          key={l.code}
          type="button"
          role="tab"
          aria-selected={value === l.code}
          onClick={() => onChange(l.code)}
          className={`rounded-full px-3 py-1.5 text-xs font-semibold transition sm:text-sm ${
            value === l.code
              ? 'bg-brand text-white shadow-sm'
              : 'bg-white text-brand/80 ring-1 ring-brand/15 hover:ring-brand/30'
          }`}
        >
          {l.native}
        </button>
      ))}
    </div>
  )
}
