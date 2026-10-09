import { useApp } from '../hooks/useApp'
import { t } from '../lib/i18n'

/** Red-flag banner with one-tap calls to India's emergency numbers (108 ambulance, 112). */
export function EmergencyBanner() {
  const { language } = useApp()
  return (
    <div role="alert" className="rounded-2xl border-2 border-red-700 bg-red-50 p-4 text-red-900">
      <p className="font-semibold">{t(language, 'emergencyTitle')}</p>
      <p className="mt-1 text-sm">{t(language, 'emergencyBody')}</p>
      <div className="mt-3 flex flex-wrap gap-2">
        <a
          href="tel:108"
          className="btn rounded-xl bg-red-700 px-4 py-2 text-sm font-semibold text-white hover:bg-red-800"
        >
          {t(language, 'call')} 108
        </a>
        <a
          href="tel:112"
          className="btn rounded-xl bg-red-700 px-4 py-2 text-sm font-semibold text-white hover:bg-red-800"
        >
          {t(language, 'call')} 112
        </a>
      </div>
    </div>
  )
}
