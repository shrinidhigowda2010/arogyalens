import { getDeviceId, historyHeaders } from '../lib/history'
import { prepareUpload } from '../lib/image'
import type {
  AnalysisResponse,
  AppLanguage,
  ChatResponse,
  DoctorSearchRequest,
  DoctorSearchResponse,
  HealthResponse,
  HistoryItem,
  SpecialtySuggestion,
  TranslateResponse,
  VoiceQueryResponse,
} from '../types'

const API_BASE = import.meta.env.VITE_API_URL?.replace(/\/$/, '') ?? ''

/** Error raised for non-2xx API responses, carrying the backend's stable error code. */
export class ApiError extends Error {
  readonly code: string
  readonly status: number

  constructor(message: string, code: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
  }
}

interface ErrorBody {
  code?: string
  message?: string
  userMessage?: string
}

async function toApiError(res: Response): Promise<ApiError> {
  let body: ErrorBody = {}
  try {
    body = (await res.json()) as ErrorBody
  } catch {
    /* non-JSON error body */
  }
  const message =
    body.userMessage ??
    (res.status === 429
      ? 'Too many requests. Please wait a minute and try again.'
      : `Request failed (${res.status}). Please try again.`)
  return new ApiError(message, body.code ?? 'HTTP_' + res.status, res.status)
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) throw await toApiError(res)
  return (await res.json()) as T
}

function url(path: string): string {
  return `${API_BASE}${path}`
}

function postJson<T>(path: string, body: unknown): Promise<T> {
  return fetch(url(path), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...historyHeaders() },
    body: JSON.stringify(body),
  }).then((res) => handleResponse<T>(res))
}

async function analyze(path: string, file: File, language: AppLanguage): Promise<AnalysisResponse> {
  const prepared = await prepareUpload(file)
  const form = new FormData()
  form.append('file', prepared, prepared.name)
  form.append('language', language)
  return handleResponse(
    await fetch(url(path), { method: 'POST', body: form, headers: historyHeaders() }),
  )
}

export async function checkHealth(): Promise<HealthResponse> {
  return handleResponse(await fetch(url('/api/health')))
}

export const analyzeDocument = (file: File, language: AppLanguage) =>
  analyze('/api/documents/analyze', file, language)
export const analyzeMedicine = (file: File, language: AppLanguage) =>
  analyze('/api/medicines/analyze', file, language)
export const analyzePrescription = (file: File, language: AppLanguage) =>
  analyze('/api/prescriptions/analyze', file, language)
export const analyzeDischarge = (file: File, language: AppLanguage) =>
  analyze('/api/discharge/analyze', file, language)

export function translateText(
  text: string,
  targetLanguage: AppLanguage,
  medicalTerm?: string,
): Promise<TranslateResponse> {
  return postJson('/api/translate', { text, targetLanguage, medicalTerm })
}

/** Asks a health question; `sessionId` grounds the answer in a scanned document when given. */
export function voiceQuery(
  query: string,
  language: AppLanguage,
  sessionId?: string,
): Promise<VoiceQueryResponse> {
  return postJson('/api/voice/query', { query, sessionId, language })
}

export function chatMessage(
  message: string,
  language: AppLanguage,
  sessionId?: string,
): Promise<ChatResponse> {
  return postJson('/api/chat', { sessionId, message, language })
}

export function suggestSpecialty(
  condition: string,
  language: AppLanguage,
): Promise<SpecialtySuggestion> {
  return postJson('/api/doctors/specialty', { condition, language })
}

export function searchDoctors(request: DoctorSearchRequest): Promise<DoctorSearchResponse> {
  return postJson('/api/doctors/search', request)
}

/** Gemini text-to-speech fallback; returns WAV audio. */
export async function fetchSpeech(text: string, language: AppLanguage): Promise<Blob> {
  const res = await fetch(url('/api/voice/tts'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text: text.slice(0, 800), language }),
  })
  if (!res.ok) throw await toApiError(res)
  return res.blob()
}

function deviceHeaders(): Record<string, string> {
  const id = getDeviceId()
  return id ? { 'X-Device-Id': id } : {}
}

/** Lists saved history for this device (empty when the device never opted in). */
export async function listHistory(): Promise<HistoryItem[]> {
  if (!getDeviceId()) return []
  return handleResponse(await fetch(url('/api/history'), { headers: deviceHeaders() }))
}

export async function deleteHistoryItem(id: number): Promise<void> {
  const res = await fetch(url(`/api/history/${id}`), { method: 'DELETE', headers: deviceHeaders() })
  if (!res.ok) throw await toApiError(res)
}

export async function clearHistory(): Promise<void> {
  if (!getDeviceId()) return
  const res = await fetch(url('/api/history'), { method: 'DELETE', headers: deviceHeaders() })
  if (!res.ok) throw await toApiError(res)
}
