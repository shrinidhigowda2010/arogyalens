import { useEffect, useMemo, useState } from 'react'
import { translateText } from '../api/client'
import { useApp } from '../context/AppContext'
import type { AppLanguage, MedicalParameter } from '../types'
import { t } from '../lib/i18n'
import { AudioPlayer } from './AudioPlayer'
import { LanguageSwitcher } from './LanguageSwitcher'

interface ExplanationPanelProps {
  parameter: MedicalParameter
  explainLang: AppLanguage
}

export function ExplanationPanel({ parameter, explainLang: initialLang }: ExplanationPanelProps) {
  const { simplifiedCopy, setSimplifiedCopy, language, setLanguage, analysis } = useApp()
  const [explainLang, setExplainLang] = useState<AppLanguage>(initialLang || language)
  const [translated, setTranslated] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const baseText = simplifiedCopy ? parameter.simpleExplanation : parameter.explanation
  const cachedTranslations = analysis?.translations

  const localTranslation = useMemo(() => {
    if (explainLang === 'en') return null
    const term = parameter.name.toLowerCase()
    if (term.includes('hba1c') && cachedTranslations?.[explainLang]) {
      return cachedTranslations[explainLang]
    }
    return null
  }, [cachedTranslations, explainLang, parameter.name])

  useEffect(() => {
    setExplainLang(initialLang || language)
  }, [initialLang, language])

  useEffect(() => {
    if (explainLang === 'en') {
      setTranslated(null)
      return
    }
    if (localTranslation) {
      setTranslated(localTranslation)
      setLoading(false)
      return
    }
    let cancelled = false
    setLoading(true)
    translateText(baseText, explainLang, parameter.name)
      .then((res) => {
        if (!cancelled) setTranslated(res.translated)
      })
      .catch(() => {
        if (!cancelled) setTranslated(null)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [baseText, explainLang, parameter.name, localTranslation])

  const displayText =
    explainLang === 'en' ? baseText : translated ?? (loading ? 'Translating…' : baseText)

  const sections = [
    {
      title: 'What is this?',
      body: `${parameter.name} is a common lab measurement. ${simplifiedCopy ? parameter.simpleExplanation.split('.')[0] + '.' : parameter.explanation.split('.')[0] + '.'}`,
    },
    {
      title: 'Why it matters',
      body: simplifiedCopy
        ? 'Doctors use this number together with your symptoms and history — one result alone rarely tells the whole story.'
        : 'Healthcare teams interpret this value alongside other tests and your medical history.',
    },
    {
      title: 'Your result on this report',
      body: `Your document shows ${parameter.value}${parameter.unit ? ' ' + parameter.unit : ''} (reference on report: ${parameter.referenceRange || 'see original'}).`,
    },
    {
      title: 'What should I do?',
      body: 'Share this report with your healthcare professional and ask what it means for you. Do not change treatment based on this explanation alone.',
    },
  ]

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <label className="flex cursor-pointer items-center gap-2 text-sm font-medium text-brand">
          <input
            type="checkbox"
            checked={simplifiedCopy}
            onChange={(e) => setSimplifiedCopy(e.target.checked)}
            className="h-4 w-4 rounded border-brand/30 text-brand focus:ring-brand/40"
          />
          {t(language, 'explain12')}
        </label>
        <AudioPlayer text={displayText} language={explainLang} label={t(language, 'listening')} />
      </div>

      <LanguageSwitcher
        value={explainLang}
        onChange={(lang) => {
          setExplainLang(lang)
          setLanguage(lang)
        }}
      />

      <div className="rounded-xl bg-brand-muted/40 p-4 text-sm leading-relaxed text-brand/90">
        {displayText}
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        {sections.map((s) => (
          <div key={s.title} className="rounded-xl border border-brand/10 bg-surface/80 p-4">
            <h4 className="text-xs font-bold uppercase tracking-wide text-brand/60">{s.title}</h4>
            <p className="mt-2 text-sm text-brand/85">{s.body}</p>
          </div>
        ))}
      </div>
    </div>
  )
}
