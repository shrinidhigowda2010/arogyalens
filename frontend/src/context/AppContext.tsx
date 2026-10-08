import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import type { AnalysisResponse, AppLanguage, StoredSession } from '../types'
import { STORAGE_KEYS } from '../lib/constants'

interface AppContextValue {
  language: AppLanguage
  setLanguage: (lang: AppLanguage) => void
  accessibilityMode: boolean
  setAccessibilityMode: (on: boolean) => void
  analysis: AnalysisResponse | null
  setAnalysis: (data: AnalysisResponse | null) => void
  recentSessions: StoredSession[]
  addSession: (session: StoredSession) => void
  simplifiedCopy: boolean
  setSimplifiedCopy: (on: boolean) => void
}

const AppContext = createContext<AppContextValue | null>(null)

function loadLanguage(): AppLanguage {
  const raw = localStorage.getItem(STORAGE_KEYS.language)
  if (raw === 'hi' || raw === 'kn' || raw === 'ta' || raw === 'te' || raw === 'mr' || raw === 'bn')
    return raw
  return 'en'
}

function loadA11y(): boolean {
  return localStorage.getItem(STORAGE_KEYS.a11y) === 'true'
}

function loadSessions(): StoredSession[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.sessions)
    if (!raw) return []
    const parsed = JSON.parse(raw) as StoredSession[]
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

export function AppProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<AppLanguage>(loadLanguage)
  const [accessibilityMode, setAccessibilityModeState] = useState(loadA11y)
  const [analysis, setAnalysis] = useState<AnalysisResponse | null>(null)
  const [recentSessions, setRecentSessions] = useState<StoredSession[]>(loadSessions)
  const [simplifiedCopy, setSimplifiedCopy] = useState(false)

  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.language, language)
    document.documentElement.lang = language === 'en' ? 'en' : language
  }, [language])

  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.a11y, String(accessibilityMode))
    document.documentElement.dataset.a11y = accessibilityMode ? 'true' : 'false'
  }, [accessibilityMode])

  const setLanguage = useCallback((lang: AppLanguage) => setLanguageState(lang), [])
  const setAccessibilityMode = useCallback((on: boolean) => setAccessibilityModeState(on), [])

  const addSession = useCallback((session: StoredSession) => {
    setRecentSessions((prev) => {
      const next = [session, ...prev.filter((s) => s.sessionId !== session.sessionId)].slice(0, 8)
      localStorage.setItem(STORAGE_KEYS.sessions, JSON.stringify(next))
      return next
    })
  }, [])

  const value = useMemo(
    () => ({
      language,
      setLanguage,
      accessibilityMode,
      setAccessibilityMode,
      analysis,
      setAnalysis,
      recentSessions,
      addSession,
      simplifiedCopy,
      setSimplifiedCopy,
    }),
    [
      language,
      setLanguage,
      accessibilityMode,
      setAccessibilityMode,
      analysis,
      recentSessions,
      addSession,
      simplifiedCopy,
    ],
  )

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>
}

export function useApp(): AppContextValue {
  const ctx = useContext(AppContext)
  if (!ctx) throw new Error('useApp must be used within AppProvider')
  return ctx
}
