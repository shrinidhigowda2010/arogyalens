import { configureAxe } from 'vitest-axe'

/**
 * axe-core configured for jsdom. Colour contrast needs a real layout engine,
 * so it is checked by design tokens (text-brand/85+ on white/surface) instead.
 */
export const axe = configureAxe({ rules: { 'color-contrast': { enabled: false } } })
