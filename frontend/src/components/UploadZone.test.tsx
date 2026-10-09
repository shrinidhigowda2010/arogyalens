import { fireEvent, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { renderWithApp } from '../test/render'
import { UploadZone } from './UploadZone'

describe('UploadZone', () => {
  it('accepts supported files', () => {
    const onSelect = vi.fn()
    renderWithApp(<UploadZone selectedFile={null} onFileSelect={onSelect} />)
    const file = new File(['%PDF-1.7'], 'report.pdf', { type: 'application/pdf' })
    fireEvent.change(screen.getByLabelText('Choose file'), { target: { files: [file] } })
    expect(onSelect).toHaveBeenCalledWith(file)
  })

  it('rejects unsupported files with an accessible error', () => {
    const onSelect = vi.fn()
    renderWithApp(<UploadZone selectedFile={null} onFileSelect={onSelect} />)
    const file = new File(['x'], 'notes.txt', { type: 'text/plain' })
    fireEvent.change(screen.getByLabelText('Choose file'), { target: { files: [file] } })
    expect(screen.getByRole('alert')).toHaveTextContent(/JPG, PNG, WEBP, or PDF/)
    expect(onSelect).not.toHaveBeenCalled()
  })
})
