import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  analyzeDischarge,
  analyzeDocument,
  analyzeMedicine,
  analyzePrescription,
  checkHealth,
} from '../api/client'
import { PrivacyShield } from '../components/PrivacyShield'
import { ProcessingProgress } from '../components/ProcessingProgress'
import { UploadZone } from '../components/UploadZone'
import { useApp } from '../hooks/useApp'
import { t } from '../lib/i18n'
import type { AnalyzeMode, AnalysisResponse, AppLanguage } from '../types'

const MODE_META: Record<
  AnalyzeMode,
  {
    titleKey: 'scanMedical' | 'scanMedicine' | 'scanRx' | 'scanDischarge'
    analyze: (file: File, language: AppLanguage) => Promise<AnalysisResponse>
  }
> = {
  report: { titleKey: 'scanMedical', analyze: analyzeDocument },
  medicine: { titleKey: 'scanMedicine', analyze: analyzeMedicine },
  prescription: { titleKey: 'scanRx', analyze: analyzePrescription },
  discharge: { titleKey: 'scanDischarge', analyze: analyzeDischarge },
}

const STEP_COUNT = 7
const STEP_MS = 160
const PRIVACY_MS = 300
const NAV_MS = 200

export default function Analyze() {
  const { mode: modeParam } = useParams<{ mode: string }>()
  const mode = (modeParam ?? 'report') as AnalyzeMode
  const meta = MODE_META[mode] ?? MODE_META.report

  const navigate = useNavigate()
  const { setAnalysis, addSession, language } = useApp()

  const [file, setFile] = useState<File | null>(null)
  const [phase, setPhase] = useState<'upload' | 'privacy' | 'processing'>('upload')
  const [shield, setShield] = useState<AnalysisResponse['privacyShield'] | null>(null)
  const [steps, setSteps] = useState<AnalysisResponse['processingSteps'] | null>(null)
  const [stepIndex, setStepIndex] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const runAnalysis = async () => {
    if (!file) {
      setError(t(language, 'needFile'))
      return
    }
    setError(null)
    setBusy(true)
    setPhase('privacy')
    setShield(null)
    setStepIndex(0)

    const tick = window.setInterval(() => {
      setStepIndex((i) => Math.min(i + 1, STEP_COUNT - 1))
    }, STEP_MS)

    try {
      const health = await checkHealth().catch(() => null)
      const isPdf = file.name.toLowerCase().endsWith('.pdf') || file.type === 'application/pdf'
      if (health && health.aiConfigured === false && !isPdf && mode !== 'report') {
        throw new Error(
          'Image recognition is not available on this server right now. Text-based PDF reports still work.',
        )
      }

      const resultPromise = meta.analyze(file, language)
      window.setTimeout(() => setPhase('processing'), PRIVACY_MS)

      const result = await resultPromise
      setShield(result.privacyShield)
      setSteps(result.processingSteps)
      setStepIndex(STEP_COUNT)
      setAnalysis(result)
      addSession({
        sessionId: result.sessionId,
        documentLabel: result.documentLabel,
        documentType: result.documentType,
        demo: false,
        visitedAt: Date.now(),
      })
      window.clearInterval(tick)
      await new Promise((r) => setTimeout(r, NAV_MS))
      navigate('/results')
    } catch (e) {
      window.clearInterval(tick)
      setPhase('upload')
      setError(
        e instanceof Error ? e.message : 'We could not analyze this document. Please try again.',
      )
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="atmosphere mx-auto max-w-2xl px-4 py-10 sm:px-6">
      <Link to="/" className="text-sm font-semibold text-brand/85 hover:text-brand">
        ← {t(language, 'backHome')}
      </Link>
      <h1 className="font-display mt-4 text-3xl font-semibold text-brand">
        {t(language, meta.titleKey)}
      </h1>
      <p className="text-muted mt-2 text-brand/85">{t(language, 'uploadHint')}</p>

      {phase === 'upload' ? (
        <>
          <div className="mt-8">
            <UploadZone selectedFile={file} onFileSelect={setFile} disabled={busy} />
          </div>
          <div className="mt-6 flex flex-wrap gap-3">
            <button
              type="button"
              disabled={busy || !file}
              onClick={() => void runAnalysis()}
              className="btn rounded-xl bg-brand px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-50"
            >
              {t(language, 'analyze')}
            </button>
          </div>
        </>
      ) : (
        <>
          <div className="mt-8">
            <PrivacyShield shield={shield} />
          </div>
          {phase === 'processing' || shield ? (
            <ProcessingProgress
              steps={steps}
              activeIndex={stepIndex}
              complete={stepIndex >= STEP_COUNT - 1 && !!shield}
            />
          ) : null}
        </>
      )}

      {error ? (
        <p className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  )
}
