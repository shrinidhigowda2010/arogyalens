# Security Policy

## Reporting
Please report vulnerabilities privately via GitHub Security Advisories on this repository. Do not open public issues for security problems.

## Controls in ArogyaLens
- **Secrets**: only `.env.example` is committed; keys (Gemini, Maps, DATABASE_URL) come from platform env vars and are sent in headers (`x-goog-api-key`, `X-Goog-Api-Key`), never in URLs or logs.
- **PII masking**: names, phone numbers, emails, Aadhaar/PAN-like IDs are masked before any AI call, before session storage and before history storage.
- **Uploads**: type detected from magic bytes (JPEG/PNG/WEBP/PDF), 15 MB limit, 413/415 errors.
- **Validation**: Bean Validation on every JSON body; uniform error JSON with no stack traces or internals.
- **Rate limiting**: per-IP token bucket on AI endpoints (`RATE_LIMIT_PER_MINUTE`, 429 + `Retry-After`).
- **Headers**: CSP, `X-Content-Type-Options`, `X-Frame-Options: DENY`, Referrer-Policy, Permissions-Policy, COOP, HSTS on HTTPS, `no-store` on `/api`.
- **CORS**: allow-list from `ALLOWED_ORIGINS`.
- **History**: opt-in only, anonymous device id, user can delete one or all entries; Postgres over TLS (`sslmode=require`).
- **Container**: runs as a non-root user.
