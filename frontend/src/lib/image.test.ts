import { describe, expect, it, vi } from 'vitest'
import { MAX_IMAGE_EDGE, fitWithin, prepareUpload } from './image'

describe('fitWithin', () => {
  it('keeps small images unchanged', () => {
    expect(fitWithin(800, 600, 2000)).toEqual({ width: 800, height: 600 })
  })
  it('scales the longest edge down and keeps aspect ratio', () => {
    expect(fitWithin(4000, 3000, 2000)).toEqual({ width: 2000, height: 1500 })
    expect(fitWithin(1000, 5000, 2000)).toEqual({ width: 400, height: 2000 })
  })
})

describe('prepareUpload', () => {
  it('leaves PDFs and small images untouched', async () => {
    const pdf = new File(['%PDF-1.7'], 'report.pdf', { type: 'application/pdf' })
    const small = new File(['x'], 'photo.png', { type: 'image/png' })
    expect(await prepareUpload(pdf)).toBe(pdf)
    expect(await prepareUpload(small)).toBe(small)
  })
})

describe('prepareUpload for large phone photos', () => {
  it('decodes upright, fits within the max edge and re-encodes as JPEG 0.85', async () => {
    const close = vi.fn()
    const decode = vi.fn().mockResolvedValue({ width: 4032, height: 3024, close })
    vi.stubGlobal('createImageBitmap', decode)
    const drawImage = vi.fn()
    const toBlob = vi.fn((cb: (b: Blob) => void) => cb(new Blob(['small'], { type: 'image/jpeg' })))
    const canvas = { width: 0, height: 0, getContext: () => ({ drawImage }), toBlob }
    const create = vi.spyOn(document, 'createElement').mockReturnValue(canvas as never)
    const big = new File([new Uint8Array(2_000_000)], 'IMG_1.jpeg', { type: 'image/jpeg' })
    const out = await prepareUpload(big)
    expect(decode).toHaveBeenCalledWith(big, { imageOrientation: 'from-image' })
    expect(canvas.width).toBe(MAX_IMAGE_EDGE)
    expect(canvas.height).toBe(1200)
    expect(toBlob).toHaveBeenCalledWith(expect.any(Function), 'image/jpeg', 0.85)
    expect(out.name).toBe('IMG_1.jpg')
    expect(out.type).toBe('image/jpeg')
    create.mockRestore()
    vi.unstubAllGlobals()
  })
})
