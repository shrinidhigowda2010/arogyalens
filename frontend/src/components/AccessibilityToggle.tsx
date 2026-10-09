import { useApp } from '../hooks/useApp'
import { t } from '../lib/i18n'

export function AccessibilityToggle() {
  const { accessibilityMode, setAccessibilityMode, language } = useApp()

  return (
    <button
      type="button"
      onClick={() => setAccessibilityMode(!accessibilityMode)}
      className={`btn rounded-lg border px-3 py-2 text-sm font-semibold transition ${
        accessibilityMode
          ? 'border-brand bg-brand text-white'
          : 'border-brand/20 bg-white text-brand hover:border-brand/40'
      }`}
      aria-pressed={accessibilityMode}
    >
      {t(language, 'accessibility')}
      {accessibilityMode ? ' ✓' : ''}
    </button>
  )
}
