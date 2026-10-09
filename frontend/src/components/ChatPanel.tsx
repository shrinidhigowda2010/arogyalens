import { useState } from 'react'
import { chatMessage } from '../api/client'
import { useApp } from '../hooks/useApp'
import type { ChatResponse } from '../types'

interface ChatPanelProps {
  sessionId: string
  compact?: boolean
}

type Msg = { role: 'user' | 'assistant'; text: string; meta?: ChatResponse }

export function ChatPanel({ sessionId, compact }: ChatPanelProps) {
  const { language, accessibilityMode } = useApp()
  const [input, setInput] = useState('')
  const [messages, setMessages] = useState<Msg[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const send = async () => {
    const text = input.trim()
    if (!text || loading) return
    setInput('')
    setError(null)
    setMessages((m) => [...m, { role: 'user', text }])
    setLoading(true)
    try {
      const res = await chatMessage(text, language, sessionId)
      setMessages((m) => [...m, { role: 'assistant', text: res.answer, meta: res }])
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not reach ArogyaLens. Try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section
      className={`rounded-2xl border border-brand/10 bg-white ${compact ? 'p-4' : 'p-6'}`}
      aria-labelledby="chat-panel-title"
    >
      <h2 id="chat-panel-title" className="font-display text-xl font-semibold text-brand">
        Grounded Q&amp;A
      </h2>
      <p className="text-muted mt-1 text-sm text-brand/85">
        Answers stay tied to your session document when possible. Not for emergencies.
      </p>

      <div
        role="log"
        aria-live="polite"
        aria-label="Conversation"
        tabIndex={0}
        className={`mt-4 space-y-3 overflow-y-auto rounded-xl bg-surface/80 p-3 ${compact ? 'max-h-48' : 'max-h-72'}`}
      >
        {messages.length === 0 ? (
          <p className="text-sm text-brand/85">
            Ask what a result means, how to prepare for a visit, or how to explain this to family.
          </p>
        ) : null}
        {messages.map((m, i) => (
          <div
            key={i}
            className={`rounded-lg px-3 py-2 text-sm ${
              m.role === 'user'
                ? 'ml-8 bg-brand text-white'
                : 'mr-8 bg-white text-brand/90 ring-1 ring-brand/10'
            }`}
          >
            {m.text}
            {m.meta ? (
              <details className="mt-2 text-xs opacity-80">
                <summary>Why am I seeing this?</summary>
                <ul className="mt-1 space-y-1">
                  {m.meta.fromDocument?.length ? (
                    <li>
                      <strong>From your document:</strong> {m.meta.fromDocument.join(' ')}
                    </li>
                  ) : null}
                  {m.meta.generalInfo?.length ? (
                    <li>
                      <strong>General health information:</strong> {m.meta.generalInfo.join(' ')}
                    </li>
                  ) : null}
                  {m.meta.aiExplanation?.length ? (
                    <li>
                      <strong>AI-generated explanation:</strong> {m.meta.aiExplanation.join(' ')}
                    </li>
                  ) : null}
                </ul>
              </details>
            ) : null}
          </div>
        ))}
        {loading ? <p className="text-sm text-brand/85 animate-pulse-soft">Thinking…</p> : null}
      </div>

      {error ? (
        <p className="mt-2 text-sm text-red-700" role="alert">
          {error}
        </p>
      ) : null}

      <form
        className="mt-3 flex gap-2"
        onSubmit={(e) => {
          e.preventDefault()
          void send()
        }}
      >
        <label htmlFor="chat-input" className="sr-only">
          Your question about this document
        </label>
        <input
          id="chat-input"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder={accessibilityMode ? 'Type your question here' : 'Ask about this document…'}
          className="min-w-0 flex-1 rounded-xl border border-brand/20 px-4 py-2.5 text-sm"
          disabled={loading}
        />
        <button
          type="submit"
          disabled={loading || !input.trim()}
          className="btn shrink-0 rounded-xl bg-brand px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50"
        >
          Send
        </button>
      </form>
    </section>
  )
}
