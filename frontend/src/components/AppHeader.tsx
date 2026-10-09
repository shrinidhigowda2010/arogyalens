import { NavLink, Link } from 'react-router-dom'
import { useApp } from '../hooks/useApp'
import { t } from '../lib/i18n'
import { AccessibilityToggle } from './AccessibilityToggle'
import { LanguageSelector } from './LanguageSelector'

const navClass = ({ isActive }: { isActive: boolean }) =>
  `rounded-lg px-3 py-2 text-sm font-semibold ${
    isActive ? 'bg-brand-muted text-brand' : 'text-brand hover:bg-brand-muted/60'
  }`

export function AppHeader() {
  const { language } = useApp()

  return (
    <header className="sticky top-0 z-40 border-b border-brand/10 bg-white/95 backdrop-blur-md">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-4 py-3 sm:px-6">
        <Link to="/" className="group flex min-w-0 flex-col" aria-label="ArogyaLens home">
          <span className="font-display text-xl font-semibold tracking-tight text-brand sm:text-2xl">
            ArogyaLens
          </span>
          <span className="text-xs font-medium text-brand/85 sm:text-sm">
            See. Understand. Hear. Act.
          </span>
        </Link>

        <nav aria-label="Main" className="flex flex-wrap items-center gap-1 sm:gap-2">
          <NavLink to="/ask" className={navClass}>
            {t(language, 'ask')}
          </NavLink>
          <NavLink to="/doctors" className={navClass}>
            {t(language, 'doctors')}
          </NavLink>
          <NavLink to="/history" className={navClass}>
            {t(language, 'history')}
          </NavLink>
          <LanguageSelector compact />
          <AccessibilityToggle />
        </nav>
      </div>
    </header>
  )
}
