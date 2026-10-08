export type DocumentType =
  | 'LAB_REPORT'
  | 'MEDICINE'
  | 'PRESCRIPTION'
  | 'DISCHARGE_SUMMARY'
  | 'UNKNOWN'

export type ParameterStatus =
  | 'WITHIN_RANGE'
  | 'OUTSIDE_RANGE'
  | 'REQUIRES_DISCUSSION'
  | 'IMPORTANT_ATTENTION'
  | 'UNKNOWN'
  | 'LOW_CONFIDENCE'

export interface PiiFinding {
  type: string
  maskedValue: string
  redacted: boolean
}

export interface PrivacyShieldDto {
  findings: PiiFinding[]
  message: string
  redacted: boolean
}

export interface ProcessingStepDto {
  id: string
  label: string
  status: string
}

export interface DashboardSummaryDto {
  total: number
  withinRange: number
  needsDiscussion: number
  importantAttention: number
}

export interface MedicalParameter {
  name: string
  value: string
  unit: string
  referenceRange: string
  status: ParameterStatus
  explanation: string
  simpleExplanation: string
  confidence: number
  lowConfidence: boolean
}

export interface TrustedSource {
  id: string
  name: string
  title: string
  description: string
  url: string
  category: string
}

export interface MedicineInfo {
  name: string
  strength: string
  dosageForm: string
  manufacturer: string
  generalUse: string
  commonSideEffects: string[]
  precautions: string[]
  warnings: string[]
  confidence: number
  sources: TrustedSource[]
}

export interface PrescriptionItem {
  medicineName: string
  strength: string
  frequency: string
  timing: string
  foodRelation: string
  morning: boolean
  afternoon: boolean
  night: boolean
  confident: boolean
  note: string
}

export interface DischargeSummary {
  reasonForAdmission: string
  treatmentPerformed: string
  importantFindings: string[]
  medicinesListed: string[]
  followUpInstructions: string[]
  warningSigns: string[]
  doctorQuestions: string[]
}

export interface AnalysisResponse {
  sessionId: string
  documentType: DocumentType
  documentLabel: string
  demo: boolean
  privacyShield: PrivacyShieldDto
  processingSteps: ProcessingStepDto[]
  pages: number
  parametersDetected: number
  parameters: MedicalParameter[]
  dashboard: DashboardSummaryDto
  doctorQuestions: string[]
  sources: TrustedSource[]
  safetyNotes: string[]
  medicine: MedicineInfo | null
  prescriptionItems: PrescriptionItem[] | null
  dischargeSummary: DischargeSummary | null
  familySummary: string
  translations: Record<string, string> | null
  originalPreviewNote: string
  aiUsed: boolean
  disclaimer: string
}

export interface ChatResponse {
  answer: string
  fromDocument: string[]
  generalInfo: string[]
  aiExplanation: string[]
  safetyNotes: string[]
}

export interface VoiceQueryResponse {
  query: string
  answer: string
  language: string
  groundedFacts: string[]
  safetyNotes: string[]
  fromDocument: boolean
}

export interface TranslateResponse {
  original: string
  targetLanguage: string
  translated: string
  allLanguages: Record<string, string>
  medicalTerm: string | null
  simpleExplanation: string | null
}

export interface SafetyValidateResponse {
  safe: boolean
  sanitizedText: string
  violations: string[]
  notes: string[]
}

export type AppLanguage = 'en' | 'hi' | 'kn' | 'ta' | 'te' | 'mr' | 'bn'

export interface StoredSession {
  sessionId: string
  documentLabel: string
  documentType: DocumentType
  demo: boolean
  visitedAt: number
}

export type AnalyzeMode = 'report' | 'medicine' | 'prescription' | 'discharge'
