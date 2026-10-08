import { useCallback, useEffect, useRef, useState } from 'react'
import { t } from '../lib/i18n'
import { useApp } from '../context/AppContext'

const ACCEPT = '.jpg,.jpeg,.png,.webp,.pdf,image/jpeg,image/png,image/webp,application/pdf'
const OK_EXT = new Set(['jpg', 'jpeg', 'png', 'webp', 'pdf'])

interface UploadZoneProps {
  onFileSelect: (file: File | null) => void
  selectedFile: File | null
  disabled?: boolean
}

function isAllowed(file: File): boolean {
  const type = (file.type || '').toLowerCase()
  if (
    type === 'application/pdf' ||
    type === 'image/jpeg' ||
    type === 'image/png' ||
    type === 'image/webp'
  ) {
    return true
  }
  const name = file.name.toLowerCase()
  const ext = name.includes('.') ? name.slice(name.lastIndexOf('.') + 1) : ''
  return OK_EXT.has(ext)
}

export function UploadZone({ onFileSelect, selectedFile, disabled }: UploadZoneProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const { language } = useApp()
  const [dragOver, setDragOver] = useState(false)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [localError, setLocalError] = useState<string | null>(null)

  useEffect(() => {
    if (!selectedFile || !selectedFile.type.startsWith('image/')) {
      setPreviewUrl(null)
      return
    }
    const url = URL.createObjectURL(selectedFile)
    setPreviewUrl(url)
    return () => URL.revokeObjectURL(url)
  }, [selectedFile])

  const pick = useCallback(
    (file: File | null) => {
      setLocalError(null)
      if (!file) {
        onFileSelect(null)
        return
      }
      if (!isAllowed(file)) {
        setLocalError('Please upload a JPG, PNG, WEBP, or PDF file.')
        return
      }
      if (file.size > 15 * 1024 * 1024) {
        setLocalError('File is too large. Please keep uploads under 15 MB.')
        return
      }
      onFileSelect(file)
    },
    [onFileSelect],
  )

  return (
    <div
      className={`rounded-2xl border-2 border-dashed px-6 py-10 text-center transition ${
        dragOver ? 'border-brand bg-brand-muted/50' : 'border-brand/25 bg-white/80'
      } ${disabled ? 'pointer-events-none opacity-60' : ''}`}
      onDragOver={(e) => {
        e.preventDefault()
        setDragOver(true)
      }}
      onDragLeave={() => setDragOver(false)}
      onDrop={(e) => {
        e.preventDefault()
        setDragOver(false)
        pick(e.dataTransfer.files?.[0] ?? null)
      }}
    >
      <input
        ref={inputRef}
        type="file"
        accept={ACCEPT}
        className="sr-only"
        disabled={disabled}
        onChange={(e) => {
          pick(e.target.files?.[0] ?? null)
          // allow re-selecting the same file
          e.currentTarget.value = ''
        }}
      />
      <p className="font-display text-lg font-semibold text-brand">{t(language, 'uploadTitle')}</p>
      <p className="text-muted mt-2 text-sm text-brand/70">{t(language, 'uploadHint')}</p>

      {previewUrl ? (
        <img
          src={previewUrl}
          alt="Selected document preview"
          className="mx-auto mt-4 max-h-48 rounded-xl border border-brand/10 object-contain"
        />
      ) : null}

      {selectedFile ? (
        <p className="mt-4 text-sm font-medium text-brand">
          {t(language, 'selected')}: {selectedFile.name} ({Math.round(selectedFile.size / 1024)} KB)
        </p>
      ) : null}

      {localError ? (
        <p className="mt-3 text-sm text-red-700" role="alert">
          {localError}
        </p>
      ) : null}

      <button
        type="button"
        disabled={disabled}
        onClick={() => inputRef.current?.click()}
        className="btn mt-5 rounded-xl bg-brand px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-brand-light focus:outline-none focus:ring-2 focus:ring-brand/40"
      >
        {t(language, 'chooseFile')}
      </button>
    </div>
  )
}
