import { Link } from 'react-router-dom'
import { ChatPanel } from '../components/ChatPanel'
import { useApp } from '../context/AppContext'
import { t } from '../lib/i18n'

export function Ask() {
  const { analysis, language } = useApp()

  return (
    <div className="atmosphere min-h-[calc(100vh-4rem)]">
      <div className="mx-auto max-w-2xl px-4 py-10 sm:px-6">
        <h1 className="font-display text-3xl font-semibold text-brand">{t(language, 'ask')}</h1>
        <p className="text-muted mt-2 text-brand/75">{t(language, 'askDesc')}</p>

        {analysis ? (
          <div className="mt-8">
            <p className="mb-4 text-sm text-brand/70">
              Using session: <strong>{analysis.documentLabel}</strong>
            </p>
            <ChatPanel sessionId={analysis.sessionId} />
          </div>
        ) : (
          <div className="mt-8 rounded-2xl border border-brand/15 bg-white/90 p-8 text-center">
            <p className="text-brand/80">{t(language, 'uploadFirst')}</p>
            <Link
              to="/analyze/report"
              className="btn mt-5 inline-block rounded-xl bg-brand px-6 py-3 text-sm font-semibold text-white"
            >
              {t(language, 'scanReport')}
            </Link>
          </div>
        )}
      </div>
    </div>
  )
}
