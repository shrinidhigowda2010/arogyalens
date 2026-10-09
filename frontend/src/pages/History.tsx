import { useCallback, useEffect, useId, useState } from 'react'
import { ApiError, clearHistory, deleteHistoryItem, listHistory } from '../api/client'
import { useApp } from '../hooks/useApp'
import { isHistoryEnabled, setHistoryEnabled } from '../lib/history'
import { t } from '../lib/i18n'
import type { HistoryItem } from '../types'

export default function History() {
  const { language } = useApp()
  const toggleId = useId()
  const [enabled, setEnabled] = useState(isHistoryEnabled)
  const [items, setItems] = useState<HistoryItem[] | null>(null)
  const [status, setStatus] = useState('')
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    try {
      setItems(await listHistory())
      setError('')
    } catch (e) {
      setItems([])
      setError(e instanceof ApiError ? e.message : 'Could not load history.')
    }
  }, [])

  useEffect(() => {
    let active = true
    listHistory()
      .then((list) => {
        if (active) setItems(list)
      })
      .catch((e: unknown) => {
        if (!active) return
        setItems([])
        setError(e instanceof ApiError ? e.message : 'Could not load history.')
      })
    return () => {
      active = false
    }
  }, [])

  const onToggle = (on: boolean) => {
    setHistoryEnabled(on)
    setEnabled(on)
  }

  const onDelete = async (item: HistoryItem) => {
    try {
      await deleteHistoryItem(item.id)
      setStatus(t(language, 'historyDeleted'))
      await load()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Could not delete.')
    }
  }

  const onDeleteAll = async () => {
    if (!window.confirm(t(language, 'historyConfirm'))) return
    try {
      await clearHistory()
      setStatus(t(language, 'historyDeleted'))
      setItems([])
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Could not delete.')
    }
  }

  return (
    <div className="atmosphere min-h-[calc(100vh-4rem)]">
      <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
        <h1 className="font-display text-3xl font-semibold text-brand">{t(language, 'history')}</h1>
        <p className="mt-2 text-brand/85">{t(language, 'historyDesc')}</p>

        <div className="mt-6 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-brand/15 bg-white/95 p-4">
          <label htmlFor={toggleId} className="flex items-center gap-3 font-semibold text-brand">
            <input
              id={toggleId}
              type="checkbox"
              checked={enabled}
              onChange={(e) => onToggle(e.target.checked)}
              className="h-5 w-5 accent-[var(--color-brand)]"
            />
            {t(language, 'historyToggle')}
          </label>
          {items && items.length > 0 ? (
            <button
              type="button"
              onClick={() => void onDeleteAll()}
              className="btn rounded-lg border border-red-700 px-3 py-2 text-sm font-semibold text-red-800"
            >
              {t(language, 'historyDeleteAll')}
            </button>
          ) : null}
        </div>

        <p role="status" aria-live="polite" className="sr-only">
          {status}
        </p>
        {error ? (
          <p role="alert" className="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-800">
            {error}
          </p>
        ) : null}

        <section aria-labelledby="history-list" className="mt-6">
          <h2 id="history-list" className="sr-only">
            {t(language, 'history')}
          </h2>
          {items === null ? (
            <p className="text-brand/85">{t(language, 'loading')}</p>
          ) : items.length === 0 ? (
            <p className="text-brand/85">
              {enabled ? t(language, 'historyEmpty') : t(language, 'historyOff')}
            </p>
          ) : (
            <ul className="space-y-3">
              {items.map((item) => (
                <li key={item.id} className="rounded-xl border border-brand/15 bg-white p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="text-xs font-semibold uppercase tracking-wide text-brand/85">
                        {item.kind === 'SCAN'
                          ? t(language, 'historyScan')
                          : t(language, 'historyChat')}{' '}
                        ·{' '}
                        <time dateTime={item.createdAt}>
                          {new Date(item.createdAt).toLocaleString()}
                        </time>
                      </p>
                      <h3
                        className="mt-1 font-semibold break-words text-brand"
                        lang={item.language}
                      >
                        {item.title}
                      </h3>
                      <p
                        className="mt-1 text-sm break-words whitespace-pre-line text-brand/85"
                        lang={item.language}
                      >
                        {item.summary}
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={() => void onDelete(item)}
                      className="btn shrink-0 rounded-lg border border-brand/30 px-3 py-1.5 text-sm font-semibold text-brand"
                    >
                      {t(language, 'historyDelete')}
                      <span className="sr-only">: {item.title}</span>
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  )
}
