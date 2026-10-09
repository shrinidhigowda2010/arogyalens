# Contributing

1. Fork and create a feature branch.
2. Backend (Java 21): `cd backend && mvn verify` — runs tests, Spotless format check, Checkstyle and the JaCoCo coverage gate. Auto-format with `mvn spotless:apply`.
3. Frontend (Node 20.19+): `cd frontend && npm ci && npm run lint && npm run typecheck && npm run format:check && npm run test:coverage && npm run build`. Auto-format with `npm run format`.
4. Tests must never call real Google APIs; mock HTTP (`MockRestServiceServer`, `vi.stubGlobal('fetch', ...)`).
5. Never commit secrets — use `.env` (git-ignored) based on `.env.example`.
6. Use small commits with conventional prefixes (`feat:`, `fix:`, `test:`, `docs:`, `chore:`).
7. Accessibility is a requirement: new UI needs labels, keyboard support and an axe check in its test.
