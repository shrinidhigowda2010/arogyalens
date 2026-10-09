import { useContext } from 'react'
import { AppContext, type AppContextValue } from '../context/appContext'

/** Access app-wide state; must be used inside {@link AppProvider}. */
export function useApp(): AppContextValue {
  const ctx = useContext(AppContext)
  if (!ctx) throw new Error('useApp must be used within AppProvider')
  return ctx
}
