# Changelog

## 1.1.0 — 2026-10-09
### Added
- Global ask bar with voice input and answers in 7 Indian languages; Gemini TTS fallback.
- Doctor finder: Gemini/rules specialty suggestion, Google Places (New) listings or deep links, 108/112 emergency banner.
- Opt-in "My history" (PII-masked) on PostgreSQL with H2 fallback.
- Gemini model fallback chain, typed AI errors, LRU+TTL response cache.
- Security: magic-byte upload checks, per-IP rate limiting, security headers, env-based CORS, SECURITY.md.
- Tooling: CI, CodeQL, Dependabot, Spotless, Checkstyle, JaCoCo, ESLint (jsx-a11y), Prettier, Vitest + axe with coverage.

### Fixed
- 500 on voice queries without a session; empty AI responses caused by whitespace in API keys.

## 1.0.0
- Initial report, medicine, prescription and discharge scanners with Privacy Shield.
