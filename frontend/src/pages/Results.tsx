import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ReportDashboard } from '../components/ReportDashboard'
import { MedicalFindingCard } from '../components/MedicalFindingCard'
import { DoctorQuestions } from '../components/DoctorQuestions'
import { SourceCard } from '../components/SourceCard'
import { SafetyBanner } from '../components/SafetyBanner'
import { WhySeeingThis } from '../components/WhySeeingThis'
import { FamilySummary } from '../components/FamilySummary'
import { ChatPanel } from '../components/ChatPanel'
import { VoiceAssistant } from '../components/VoiceAssistant'
import { DocumentPreview } from '../components/DocumentPreview'
import { MedicineCard } from '../components/MedicineCard'
import { PrescriptionSchedule } from '../components/PrescriptionSchedule'
import { DischargeSections } from '../components/DischargeSections'
import { LanguageSwitcher } from '../components/LanguageSwitcher'
import { AudioPlayer } from '../components/AudioPlayer'
import { useApp } from '../context/AppContext'
import { t } from '../lib/i18n'
import type { AppLanguage } from '../types'

export function Results() {
  const { analysis, language, setLanguage } = useApp()
  const navigate = useNavigate()
  const [explainLang, setExplainLang] = useState<AppLanguage>(language)

  useEffect(() => {
    if (!analysis) {
      navigate('/', { replace: true })
    }
  }, [analysis, navigate])

  useEffect(() => {
    setExplainLang(language)
  }, [language])

  if (!analysis) {
    return (
      <p className="p-8 text-center text-brand/70">
        {t(language, 'noResults')}{' '}
        <Link to="/analyze/report" className="font-semibold underline">
          {t(language, 'uploadFirst')}
        </Link>
      </p>
    )
  }

  const sampleTranslation =
    analysis.translations?.[explainLang] ?? analysis.translations?.en ?? ''

  return (
    <div className="min-h-screen bg-surface">
      <div className="mx-auto max-w-6xl space-y-10 px-4 py-8 sm:px-6">
        <header>
          <Link to="/" className="text-sm font-semibold text-brand/70 hover:text-brand">
            ← {t(language, 'backHome')}
          </Link>
          <h1 className="font-display mt-3 text-3xl font-semibold text-brand">
            {analysis.documentLabel}
          </h1>
          <p className="text-muted mt-1 text-sm text-brand/70">
            Session {analysis.sessionId.slice(0, 8)}… · {analysis.pages}{' '}
            {analysis.pages === 1 ? 'page' : 'pages'}
            {analysis.aiUsed ? ' · AI-assisted' : ' · Local extraction'}
          </p>
          <p className="mt-2 text-sm italic text-brand/60">{analysis.disclaimer}</p>
        </header>

        <DocumentPreview note={analysis.originalPreviewNote} />
        <SafetyBanner notes={analysis.safetyNotes} />

        {analysis.documentType === 'LAB_REPORT' ? (
          <>
            <ReportDashboard dashboard={analysis.dashboard} />
            <section>
              <h2 className="font-display text-xl font-semibold text-brand">
                {t(language, 'yourResults')}
              </h2>
              <div className="mt-4 space-y-3">
                {analysis.parameters.map((p) => (
                  <MedicalFindingCard
                    key={p.name}
                    parameter={p}
                    explainLang={explainLang}
                    defaultOpen={p.name.toLowerCase().includes('hba1c') || analysis.parameters.length <= 3}
                  />
                ))}
              </div>
            </section>
          </>
        ) : null}

        {analysis.documentType === 'MEDICINE' && analysis.medicine ? (
          <MedicineCard medicine={analysis.medicine} />
        ) : null}

        {analysis.documentType === 'PRESCRIPTION' && analysis.prescriptionItems ? (
          <PrescriptionSchedule items={analysis.prescriptionItems} />
        ) : null}

        {analysis.documentType === 'DISCHARGE_SUMMARY' && analysis.dischargeSummary ? (
          <DischargeSections summary={analysis.dischargeSummary} />
        ) : null}

        <section className="rounded-2xl border border-brand/10 bg-white p-6">
          <h2 className="font-display text-xl font-semibold text-brand">
            {t(language, 'multilingual')}
          </h2>
          <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
            <LanguageSwitcher
              value={explainLang}
              onChange={(lang) => {
                setExplainLang(lang)
                setLanguage(lang)
              }}
            />
            <AudioPlayer
              text={sampleTranslation || analysis.parameters?.[0]?.explanation || ''}
              language={explainLang}
            />
          </div>
          {sampleTranslation ? (
            <p className="mt-4 text-sm leading-relaxed text-brand/85">{sampleTranslation}</p>
          ) : (
            <p className="mt-4 text-sm text-brand/70">
              Open a finding below and switch language to hear a localized explanation.
            </p>
          )}
        </section>

        {analysis.sources?.length ? (
          <section>
            <h2 className="font-display text-xl font-semibold text-brand">{t(language, 'sources')}</h2>
            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              {analysis.sources.map((s) => (
                <SourceCard key={s.id} source={s} />
              ))}
            </div>
          </section>
        ) : null}

        <DoctorQuestions questions={analysis.doctorQuestions} />
        <WhySeeingThis />
        <FamilySummary summary={analysis.familySummary} />

        <div className="grid gap-6 lg:grid-cols-2">
          <ChatPanel sessionId={analysis.sessionId} />
          <VoiceAssistant sessionId={analysis.sessionId} />
        </div>
      </div>
    </div>
  )
}
