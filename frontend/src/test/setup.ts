import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach, expect } from 'vitest'
import * as axeMatchers from 'vitest-axe/matchers'

expect.extend(axeMatchers)

afterEach(() => {
  cleanup()
  localStorage.clear()
})

// jsdom has no canvas; axe probes it and logs noisy "not implemented" errors.
Object.defineProperty(HTMLCanvasElement.prototype, 'getContext', { value: () => null })
