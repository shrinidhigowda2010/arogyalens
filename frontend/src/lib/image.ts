/** Longest edge, in pixels, of images sent for analysis. Plenty for reading labels and reports. */
export const MAX_IMAGE_EDGE = 2000
/** Images smaller than this are uploaded unchanged. */
export const COMPRESS_THRESHOLD_BYTES = 1_000_000

/** Scales (width, height) down to fit within `max` on the longest edge, keeping aspect ratio. */
export function fitWithin(
  width: number,
  height: number,
  max: number,
): { width: number; height: number } {
  const longest = Math.max(width, height)
  if (longest <= max) return { width, height }
  const scale = max / longest
  return { width: Math.round(width * scale), height: Math.round(height * scale) }
}

/**
 * Downscales and re-encodes large photos as JPEG before upload, which makes uploads faster on
 * mobile data and uses less AI quota. PDFs, small files and unsupported browsers are left as-is.
 */
export async function prepareUpload(file: File): Promise<File> {
  if (!file.type.startsWith('image/') || file.size < COMPRESS_THRESHOLD_BYTES) return file
  if (typeof createImageBitmap !== 'function' || typeof document === 'undefined') return file
  try {
    const bitmap = await createImageBitmap(file)
    const { width, height } = fitWithin(bitmap.width, bitmap.height, MAX_IMAGE_EDGE)
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const ctx = canvas.getContext('2d')
    if (!ctx) return file
    ctx.drawImage(bitmap, 0, 0, width, height)
    bitmap.close()
    const blob = await new Promise<Blob | null>((resolve) =>
      canvas.toBlob(resolve, 'image/jpeg', 0.85),
    )
    if (!blob || blob.size >= file.size) return file
    const name = file.name.replace(/\.[^.]+$/, '') + '.jpg'
    return new File([blob], name, { type: 'image/jpeg' })
  } catch {
    return file
  }
}
