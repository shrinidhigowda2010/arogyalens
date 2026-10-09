import { Link } from 'react-router-dom'
import { AskBar } from '../components/AskBar'
import { useApp } from '../hooks/useApp'
import { t } from '../lib/i18n'

export default function Home() {
  const { recentSessions, language } = useApp()

  const actions = [
    {
      to: '/analyze/report',
      title: t(language, 'scanMedical'),
      desc: t(language, 'scanMedicalDesc'),
    },
    {
      to: '/analyze/medicine',
      title: t(language, 'scanMedicine'),
      desc: t(language, 'scanMedicineDesc'),
    },
    { to: '/analyze/prescription', title: t(language, 'scanRx'), desc: t(language, 'scanRxDesc') },
    {
      to: '/analyze/discharge',
      title: t(language, 'scanDischarge'),
      desc: t(language, 'scanDischargeDesc'),
    },
    { to: '/ask', title: t(language, 'ask'), desc: t(language, 'askDesc') },
    { to: '/doctors', title: t(language, 'doctors'), desc: t(language, 'doctorsDesc') },
  ]

  return (
    <div className="atmosphere min-h-[calc(100vh-4rem)]">
      <section className="mx-auto max-w-6xl px-4 pb-16 pt-10 sm:px-6 sm:pt-16">
        <div className="animate-fade-up max-w-3xl">
          <p className="font-display text-5xl font-semibold tracking-tight text-brand sm:text-6xl lg:text-7xl">
            ArogyaLens
          </p>
          <p className="mt-3 text-sm font-semibold uppercase tracking-widest text-brand/85">
            {t(language, 'positioning')}
          </p>
          <h1 className="font-display mt-5 text-2xl font-medium leading-snug text-brand/90 sm:text-3xl">
            {t(language, 'hero')}
          </h1>
          <p className="mt-5 max-w-xl text-lg text-brand/85">{t(language, 'heroBody')}</p>
          <div className="mt-8">
            <AskBar />
          </div>
          <div className="mt-6 flex flex-wrap gap-3">
            <Link
              to="/analyze/report"
              className="btn rounded-xl bg-brand px-6 py-3 text-base font-semibold text-white shadow-md transition hover:bg-brand-light "
            >
              {t(language, 'scanReport')}
            </Link>
          </div>
        </div>

        <div className="mt-16 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {actions.map((a) => (
            <Link
              key={a.to}
              to={a.to}
              className="group rounded-2xl border border-brand/10 bg-white/70 p-5 transition hover:border-brand/25 hover:bg-white hover:shadow-sm"
            >
              <h2 className="font-display text-lg font-semibold text-brand group-hover:text-brand-light">
                {a.title}
              </h2>
              <p className="text-muted mt-2 text-sm text-brand/85">{a.desc}</p>
            </Link>
          ))}
        </div>

        {recentSessions.length > 0 ? (
          <section className="mt-16" aria-labelledby="recent-sessions">
            <h2 id="recent-sessions" className="font-display text-2xl font-semibold text-brand">
              {t(language, 'recent')}
            </h2>
            <ul className="mt-4 divide-y divide-brand/10 rounded-2xl border border-brand/10 bg-white/80">
              {recentSessions.map((s) => (
                <li key={s.sessionId}>
                  <Link
                    to="/results"
                    className="flex flex-wrap items-center justify-between gap-2 px-5 py-4 transition hover:bg-brand-muted/30"
                  >
                    <span className="font-medium text-brand">{s.documentLabel}</span>
                    <span className="text-xs text-brand/85">
                      {new Date(s.visitedAt).toLocaleString()}
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          </section>
        ) : null}

        <footer className="mt-20 max-w-2xl border-t border-brand/10 pt-10">
          <blockquote className="font-display text-xl italic text-brand/90">
            {t(language, 'footerQuote')}
          </blockquote>
          <p className="mt-4 font-display text-lg font-semibold text-brand">
            ArogyaLens — {t(language, 'tagline')}
          </p>
          <p className="text-muted mt-2 text-sm text-brand/85">{t(language, 'footerMission')}</p>
        </footer>
      </section>
    </div>
  )
}
