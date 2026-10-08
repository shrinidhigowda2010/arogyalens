import { Link, useLocation } from 'react-router-dom'
import { AccessibilityToggle } from './AccessibilityToggle'
import { LanguageSelector } from './LanguageSelector'

export function AppHeader() {
  const location = useLocation()

  return (
    <header className="sticky top-0 z-40 border-b border-brand/10 bg-white/90 backdrop-blur-md">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-4 py-3 sm:px-6">
        <Link to="/" className="group flex min-w-0 flex-col">
          <span className="font-display text-xl font-semibold tracking-tight text-brand sm:text-2xl">
            ArogyaLens
          </span>
          <span className="text-muted text-xs font-medium text-brand/70 transition group-hover:text-brand/90 sm:text-sm">
            See. Understand. Hear. Act.
          </span>
        </Link>

        <nav className="flex flex-wrap items-center gap-2 sm:gap-3" aria-label="Main">
          <Link
            to="/ask"
            className={`hidden rounded-lg px-3 py-2 text-sm font-semibold sm:inline ${
              location.pathname === '/ask'
                ? 'bg-brand-muted text-brand'
                : 'text-brand/80 hover:bg-brand-muted/60'
            }`}
          >
            Ask ArogyaLens
          </Link>
          <LanguageSelector compact />
          <AccessibilityToggle />
        </nav>
      </div>
    </header>
  )
}
