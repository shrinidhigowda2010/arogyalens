import { createContext } from 'react'
import type { AnalysisResponse, AppLanguage, StoredSession } from '../types'

export interface AppContextValue {
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

export const AppContext = createContext<AppContextValue | null>(null)
