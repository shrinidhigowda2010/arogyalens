import type {
  AnalysisResponse,
  AppLanguage,
  ChatResponse,
  SafetyValidateResponse,
  TranslateResponse,
  VoiceQueryResponse,
} from '../types'

const API_BASE = (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, '') ?? ''

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let detail = res.statusText
    try {
      const body = await res.json()
      if (body?.userMessage) detail = body.userMessage
      else if (body?.message) detail = body.message
      else if (typeof body === 'string') detail = body
    } catch {
      /* ignore */
    }
    throw new Error(detail || `Request failed (${res.status})`)
  }
  return res.json() as Promise<T>
}

function url(path: string): string {
  return `${API_BASE}${path}`
}

async function analyze(
  path: string,
  file: File,
  language: AppLanguage,
): Promise<AnalysisResponse> {
  const form = new FormData()
  form.append('file', file, file.name)
  form.append('language', language)
  form.append('demo', 'false')
  return handleResponse(await fetch(url(path), { method: 'POST', body: form }))
}

export async function checkHealth(): Promise<{ status: string; aiConfigured?: boolean }> {
  return handleResponse(await fetch(url('/api/health')))
}

export async function analyzeDocument(
  file: File,
  language: AppLanguage,
): Promise<AnalysisResponse> {
  return analyze('/api/documents/analyze', file, language)
}

export async function analyzeMedicine(
  file: File,
  language: AppLanguage,
): Promise<AnalysisResponse> {
  return analyze('/api/medicines/analyze', file, language)
}

export async function analyzePrescription(
  file: File,
  language: AppLanguage,
): Promise<AnalysisResponse> {
  return analyze('/api/prescriptions/analyze', file, language)
}

export async function analyzeDischarge(
  file: File,
  language: AppLanguage,
): Promise<AnalysisResponse> {
  return analyze('/api/discharge/analyze', file, language)
}

export async function translateText(
  text: string,
  targetLanguage: AppLanguage,
  medicalTerm?: string,
): Promise<TranslateResponse> {
  return handleResponse(
    await fetch(url('/api/translate'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text, targetLanguage, medicalTerm }),
    }),
  )
}

export async function voiceQuery(
  query: string,
  sessionId: string,
  language: AppLanguage,
): Promise<VoiceQueryResponse> {
  return handleResponse(
    await fetch(url('/api/voice/query'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query, sessionId, language }),
    }),
  )
}

export async function chatMessage(
  sessionId: string,
  message: string,
  language: AppLanguage,
): Promise<ChatResponse> {
  return handleResponse(
    await fetch(url('/api/chat'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId, message, language }),
    }),
  )
}

export async function fetchDoctorQuestions(
  sessionId: string,
  language: AppLanguage,
): Promise<Record<string, string[]>> {
  return handleResponse(
    await fetch(url('/api/doctor-questions'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId, language }),
    }),
  )
}

export async function validateSafety(text: string): Promise<SafetyValidateResponse> {
  return handleResponse(
    await fetch(url('/api/safety/validate'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text }),
    }),
  )
}

export async function fetchSource(id: string): Promise<unknown> {
  return handleResponse(await fetch(url(`/api/sources/${encodeURIComponent(id)}`)))
}
