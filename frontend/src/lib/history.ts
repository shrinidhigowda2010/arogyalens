const ENABLED_KEY = 'arogyalens.history.enabled'
const DEVICE_KEY = 'arogyalens.deviceId'

/** Whether the user opted in to saving PII-masked history on the server. */
export function isHistoryEnabled(): boolean {
  return localStorage.getItem(ENABLED_KEY) === 'true'
}

export function setHistoryEnabled(enabled: boolean): void {
  localStorage.setItem(ENABLED_KEY, String(enabled))
  if (enabled) getOrCreateDeviceId()
}

/** Anonymous random id for this browser; never derived from personal data. */
export function getDeviceId(): string | null {
  return localStorage.getItem(DEVICE_KEY)
}

export function getOrCreateDeviceId(): string {
  const existing = getDeviceId()
  if (existing) return existing
  const id = crypto.randomUUID()
  localStorage.setItem(DEVICE_KEY, id)
  return id
}

/** Header that tells the backend to record this request (only when the user opted in). */
export function historyHeaders(): Record<string, string> {
  return isHistoryEnabled() ? { 'X-Device-Id': getOrCreateDeviceId() } : {}
}
