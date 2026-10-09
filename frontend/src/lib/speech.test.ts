import { describe, expect, it } from 'vitest'
import { detectLanguage, findVoice, speechLocale } from './speech'

const voice = (lang: string) => ({ lang, name: lang }) as SpeechSynthesisVoice

describe('speechLocale', () => {
  it('maps every app language to an Indian locale', () => {
    expect(speechLocale('en')).toBe('en-IN')
    expect(speechLocale('kn')).toBe('kn-IN')
    expect(speechLocale('bn')).toBe('bn-IN')
  })
})

describe('detectLanguage', () => {
  it('detects Indic scripts', () => {
    expect(detectLanguage('ಮಧುಮೇಹ ಎಂದರೇನು', 'en')).toBe('kn')
    expect(detectLanguage('நீரிழிவு என்றால் என்ன', 'en')).toBe('ta')
    expect(detectLanguage('మధుమేహం అంటే ఏమిటి', 'en')).toBe('te')
    expect(detectLanguage('ডায়াবেটিস কী', 'en')).toBe('bn')
    expect(detectLanguage('मधुमेह क्या है', 'en')).toBe('hi')
  })

  it('keeps Marathi when Devanagari is typed in Marathi mode', () => {
    expect(detectLanguage('मधुमेह म्हणजे काय', 'mr')).toBe('mr')
  })

  it('returns null for Latin text', () => {
    expect(detectLanguage('what is diabetes', 'hi')).toBeNull()
  })
})

describe('findVoice', () => {
  it('prefers the exact locale, then the language prefix', () => {
    expect(findVoice('hi', [voice('en-US'), voice('hi-IN')])?.lang).toBe('hi-IN')
    expect(findVoice('ta', [voice('ta_LK')])?.lang).toBe('ta_LK')
    expect(findVoice('kn', [voice('en-IN')])).toBeNull()
  })
})
