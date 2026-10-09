import { DoctorFinder } from '../components/DoctorFinder'
import { useApp } from '../hooks/useApp'
import { t } from '../lib/i18n'

export default function Doctors() {
  const { language } = useApp()
  return (
    <div className="atmosphere min-h-[calc(100vh-4rem)]">
      <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
        <h1 className="font-display text-3xl font-semibold text-brand">{t(language, 'doctors')}</h1>
        <p className="mt-2 text-brand/85">{t(language, 'doctorsDesc')}</p>
        <div className="mt-8">
          <DoctorFinder />
        </div>
      </div>
    </div>
  )
}
