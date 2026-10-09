import { AskBar } from '../components/AskBar'
import { ChatPanel } from '../components/ChatPanel'
import { useApp } from '../hooks/useApp'
import { t } from '../lib/i18n'

export default function Ask() {
  const { analysis, language } = useApp()

  return (
    <div className="atmosphere min-h-[calc(100vh-4rem)]">
      <div className="mx-auto max-w-2xl space-y-8 px-4 py-10 sm:px-6">
        <div>
          <h1 className="font-display text-3xl font-semibold text-brand">{t(language, 'ask')}</h1>
          <p className="mt-2 text-brand/85">{t(language, 'askHint')}</p>
        </div>
        <AskBar sessionId={analysis?.sessionId} />
        {analysis ? (
          <div>
            <p className="mb-4 text-sm text-brand/85">
              Using your scan: <strong>{analysis.documentLabel}</strong>
            </p>
            <ChatPanel sessionId={analysis.sessionId} />
          </div>
        ) : null}
      </div>
    </div>
  )
}
