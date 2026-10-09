import { useCallback, useId, useState, type FormEvent } from 'react'
import { voiceQuery } from '../api/client'
import { useApp } from '../hooks/useApp'
import { useSpeechRecognition } from '../hooks/useSpeechRecognition'
import { t } from '../lib/i18n'
import { detectLanguage } from '../lib/speech'
import type { VoiceQueryResponse } from '../types'
import { AudioPlayer } from './AudioPlayer'
import { EmergencyBanner } from './EmergencyBanner'
import { LanguageSelector } from './LanguageSelector'

interface AskBarProps {
  /** When set, answers are grounded in this scanned document. */
  sessionId?: string
  /** Heading level text; omitted for the compact home-page variant. */
  title?: string
}

/**
 * "Ask anything" search bar with voice input and read-aloud answers in the selected language.
 * Answers are announced to screen readers through a polite live region.
 */
export function AskBar({ sessionId, title }: AskBarProps) {
  const { language, setLanguage } = useApp()
  const inputId = useId()
  const hintId = useId()
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<VoiceQueryResponse | null>(null)

  const ask = useCallback(
    async (text: string) => {
      const q = text.trim()
      if (!q || loading) return
      const detected = detectLanguage(q, language)
      const lang = detected ?? language
      if (detected && detected !== language) setLanguage(detected)
      setLoading(true)
      setError(null)
      try {
        setResult(await voiceQuery(q, lang, sessionId))
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Could not get an answer. Please try again.')
      } finally {
        setLoading(false)
      }
    },
    [language, loading, sessionId, setLanguage],
  )

  const onTranscript = useCallback(
    (transcript: string) => {
      setQuery(transcript)
      void ask(transcript)
    },
    [ask],
  )
  const speech = useSpeechRecognition(language, onTranscript)

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    void ask(query)
  }

  return (
    <section
      aria-label={title ? undefined : t(language, 'askLabel')}
      aria-labelledby={title ? `${inputId}-title` : undefined}
      className="rounded-2xl border border-brand/15 bg-white/95 p-4 shadow-sm sm:p-5"
    >
      {title ? (
        <h2 id={`${inputId}-title`} className="font-display mb-3 text-xl font-semibold text-brand">
          {title}
        </h2>
      ) : null}
      <form role="search" onSubmit={onSubmit} className="flex flex-col gap-3">
        <label htmlFor={inputId} className="text-sm font-semibold text-brand">
          {t(language, 'askLabel')}
        </label>
        <div className="flex flex-col gap-2 sm:flex-row">
          <input
            id={inputId}
            type="search"
            lang={language}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={t(language, 'askPlaceholder')}
            aria-describedby={hintId}
            maxLength={1000}
            autoComplete="off"
            className="min-w-0 flex-1 rounded-xl border border-brand/30 bg-white px-4 py-3 text-base text-brand placeholder:text-brand/85"
          />
          <div className="flex gap-2">
            {speech.supported ? (
              <button
                type="button"
                onClick={speech.listening ? speech.stop : speech.start}
                aria-pressed={speech.listening}
                aria-label={speech.listening ? t(language, 'micStop') : t(language, 'micStart')}
                className={`btn rounded-xl px-4 py-3 text-base font-semibold ${
                  speech.listening ? 'bg-red-700 text-white' : 'bg-brand-muted text-brand'
                }`}
              >
                <span aria-hidden="true">{speech.listening ? '■' : '🎤'}</span>
              </button>
            ) : null}
            <button
              type="submit"
              disabled={loading || !query.trim()}
              className="btn flex-1 rounded-xl bg-brand px-5 py-3 text-base font-semibold text-white disabled:opacity-60 sm:flex-none"
            >
              {loading ? t(language, 'thinking') : t(language, 'askButton')}
            </button>
          </div>
        </div>
        <div className="flex flex-wrap items-center justify-between gap-2">
          <p id={hintId} className="text-xs text-brand/85">
            {speech.supported ? t(language, 'askHint') : t(language, 'micUnsupported')}
          </p>
          <LanguageSelector label={t(language, 'answerLanguage')} compact />
        </div>
      </form>

      <div aria-live="polite" aria-busy={loading} className="mt-3 space-y-3">
        {speech.listening ? (
          <p className="text-sm font-medium text-brand">{t(language, 'micListening')}</p>
        ) : null}
        {speech.error ? <p className="text-sm text-red-800">{speech.error}</p> : null}
        {error ? (
          <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800">
            {error}
          </p>
        ) : null}
        {result?.emergency ? <EmergencyBanner /> : null}
        {result ? (
          <article lang={result.language} className="rounded-xl bg-brand-muted/60 p-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h3 className="text-sm font-bold uppercase tracking-wide text-brand">
                {t(language, 'answerHeading')}
              </h3>
              <AudioPlayer
                text={result.answer}
                language={language}
                label={t(language, 'listen')}
                stopLabel={t(language, 'stop')}
              />
            </div>
            <p className="mt-2 whitespace-pre-line text-base leading-relaxed text-brand">
              {result.answer}
            </p>
            {result.safetyNotes.length ? (
              <ul className="mt-2 list-disc pl-5 text-xs text-brand/85">
                {result.safetyNotes.map((n) => (
                  <li key={n}>{n}</li>
                ))}
              </ul>
            ) : null}
            {result.sources?.length ? (
              <div className="mt-3">
                <h4 className="text-xs font-semibold text-brand">
                  {t(language, 'sourcesHeading')}
                </h4>
                <ul className="mt-1 flex flex-wrap gap-2">
                  {result.sources.map((s) => (
                    <li key={s.id}>
                      <a
                        href={s.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-xs font-semibold text-brand underline"
                      >
                        {s.name}
                        <span className="sr-only"> (opens in a new tab)</span>
                      </a>
                    </li>
                  ))}
                </ul>
              </div>
            ) : null}
          </article>
        ) : null}
      </div>
    </section>
  )
}
