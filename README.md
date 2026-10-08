# ArogyaLens

**See. Understand. Hear. Act.**

The AI accessibility layer for healthcare.

ArogyaLens helps people understand medical reports, prescriptions, medicine packages, and discharge documents — without pretending to diagnose or replace a doctor.

---

## Problem

Healthcare information is available, but many people cannot understand it because it is too technical, written only in English, difficult to read, or presented in a format that is not accessible to them.

## Solution

ArogyaLens is an **AI accessibility layer between healthcare information and the patient**:

> **See → Understand → Translate → Hear → Verify → Prepare → Act**

It explains what documents say, translates meaning carefully, reads aloud, cites trusted sources, and prepares questions for a healthcare professional.

> We don't replace the doctor. We make healthcare easier to understand.

---

## Features

- **Medical report scanner** — multimodal understanding of labs (tables, values, units, ranges)
- **Privacy Shield** — detects and masks common PII before AI processing
- **Structured health dashboard** — within / discuss / important attention (non-diagnostic)
- **Medical term explainer** + **Explain like I'm 12**
- **Multilingual explanations** — English, Hindi, Kannada, Tamil, Telugu, Marathi, Bengali
- **Voice accessibility** — listen (TTS) and ask by voice (browser STT + backend Q&A)
- **Ask my doctor** — grounded question lists with copy/share
- **Evidence / trust layer** — WHO, MoHFW, AIIMS, IPC, CDSCO links (no fabricated citations)
- **Medicine scanner** — general informational medicine context
- **Prescription interpreter** + visual morning/afternoon/night schedule
- **Discharge summary mode**
- **Family summary** and **Accessibility mode**
- **Real document upload** — JPG / PNG / WEBP / PDF recognition (Gemini for images; local PDF text parsing without AI)
- **AI safety layer** — blocks diagnosis claims and medication-change instructions

---

## Architecture

```mermaid
flowchart TD
  UI[React Frontend] -->|REST multipart/JSON| API[Spring Boot Controllers]
  API --> Doc[DocumentAnalysisService]
  API --> Med[MedicineService]
  API --> Rx[PrescriptionService]
  API --> Dis[DischargeSummaryService]
  Doc --> Priv[PrivacyService]
  Doc --> AI[GeminiService + PromptLibrary]
  Doc --> Safe[SafetyValidationService]
  Doc --> Src[SourceService]
  Doc --> Sess[SessionService]
  AI -->|fallback| Demo[DemoDataService]
```

### Backend package layout

```text
com.arogyalens
 ├── controller
 ├── service
 ├── dto
 ├── model
 ├── config
 ├── ai
 ├── privacy
 ├── safety
 ├── source
 ├── demo
 ├── exception
 └── util
```

---

## Technology Stack

| Layer | Tech |
|-------|------|
| Frontend | React, TypeScript, Vite, Tailwind CSS |
| Backend | Java 21, Spring Boot 3.3 |
| AI | Google Gemini (optional; demo works offline) |
| Privacy / Safety | Custom Java services |

---

## Setup

### Prerequisites

- Node.js 20+
- Java 21
- Maven 3.9+

### Environment Variables

Copy `.env.example` to `.env` in the project root (and/or export variables):

| Variable | Description |
|----------|-------------|
| `GEMINI_API_KEY` | Google Gemini API key (**required for image recognition**) |
| `GEMINI_MODEL` | Default `gemini-2.0-flash` |
| `AI_ENABLED` | `true` / `false` |
| `SERVER_PORT` | Backend port (default `8088`) |
| `VITE_API_URL` | Backend URL; leave empty to use Vite proxy |

**Never commit real API keys.**

---

## Running Locally

### Backend

```bash
cd backend
# Copy root .env.example → .env and set:
export GEMINI_API_KEY=your_key_here   # required for photos/images
mvn -Dmaven.repo.local=../.m2 spring-boot:run
```

API: `http://localhost:8088` (override with `SERVER_PORT`)

### Frontend

```bash
cd frontend
npm install
npm run dev
```

App: `http://localhost:5173`

### Upload flow

1. Choose language in the header
2. Open **Scan a report** (or Medicine / Prescription / Discharge)
3. Upload a JPG/PNG/WEBP/PDF and click **Analyze document**
4. Review Privacy Shield → structured results → switch language → Listen

**Note:** Text-based lab PDFs work without Gemini. Photos of reports/medicines need `GEMINI_API_KEY` in `.env`, then restart the backend.

### Tests

```bash
cd backend
mvn test
```

---

## Privacy

- Minimal collection; sessions are in-memory and expire
- Common PII patterns are masked before AI processing where detected
- Uploaded files are processed temporarily and **not stored permanently by default**
- Medical document contents are not written to application logs
- We do **not** claim “your data can never be stored”

Wording used in product:

> ArogyaLens automatically detects and masks common personal identifiers before processing.

---

## Safety

`SafetyValidationService` reviews outbound text to reduce:

- Unsupported diagnosis claims (“You have diabetes”)
- Medication start/stop/dose-change instructions

Sensitive findings surface:

> ⚠️ Important — ArogyaLens cannot diagnose your condition or determine treatment from this document alone.

---

## Limitations

- Prototype for hackathon / early product validation
- PII detection covers common patterns, not every identifier
- Without `GEMINI_API_KEY`, image uploads cannot be recognized (text PDFs still work)
- Browser speech recognition/TTS quality depends on the device and OS voices
- Not a medical device; not for emergencies or clinical decision-making

---

## Future Roadmap

- On-device OCR + redaction pipeline for stronger privacy
- Clinician-verified explanation templates per lab panel
- Caregiver / family shared sessions with consent
- Offline-first language packs for rural connectivity
- Hospital EHR integration with explicit patient authorization

---

## API Overview

```text
POST /api/documents/analyze
POST /api/medicines/analyze
POST /api/prescriptions/analyze
POST /api/discharge/analyze
POST /api/translate
POST /api/voice/query
POST /api/chat
POST /api/doctor-questions
POST /api/safety/validate
GET  /api/sources/{id}
GET  /api/health
```

---

## License

Built for PromptWars — AI for Healthcare Accessibility.
