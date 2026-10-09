import { LANGUAGES } from '../lib/constants'
import type { AppLanguage } from '../types'

interface LanguageSwitcherProps {
  value: AppLanguage
  onChange: (lang: AppLanguage) => void
  label?: string
}

/** Row of toggle buttons for choosing the explanation language. */
export function LanguageSwitcher({
  value,
  onChange,
  label = 'Explanation language',
}: LanguageSwitcherProps) {
  return (
    <div role="group" aria-label={label} className="flex flex-wrap gap-2">
      {LANGUAGES.map((l) => (
        <button
          key={l.code}
          type="button"
          lang={l.code}
          aria-pressed={value === l.code}
          onClick={() => onChange(l.code)}
          className={`rounded-full px-3 py-1.5 text-xs font-semibold transition sm:text-sm ${
            value === l.code
              ? 'bg-brand text-white shadow-sm'
              : 'bg-white text-brand ring-1 ring-brand/25 hover:ring-brand/50'
          }`}
        >
          {l.native}
        </button>
      ))}
    </div>
  )
}
