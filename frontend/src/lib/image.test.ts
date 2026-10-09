import { describe, expect, it } from 'vitest'
import { fitWithin, prepareUpload } from './image'

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
