import { useApp } from '../hooks/useApp'
import { t } from '../lib/i18n'

/** First focusable element: lets keyboard users jump past the header. */
export function SkipLink() {
  const { language } = useApp()
  return (
    <a
      href="#main"
      className="sr-only z-50 rounded-lg bg-brand px-4 py-2 font-semibold text-white focus:not-sr-only focus:fixed focus:left-3 focus:top-3"
    >
      {t(language, 'skipToContent')}
    </a>
  )
}
