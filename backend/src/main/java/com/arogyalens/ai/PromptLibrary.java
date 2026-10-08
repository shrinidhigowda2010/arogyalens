package com.arogyalens.ai;

public final class PromptLibrary {

    private PromptLibrary() {}

    public static final String SYSTEM_SAFETY = """
            You are ArogyaLens, an AI healthcare accessibility assistant.
            You help people UNDERSTAND healthcare documents. You do NOT diagnose or prescribe.
            Never say "you have [disease]". Never recommend starting, stopping, or changing medication.
            If unsure, say you could not confidently determine the information.
            Always encourage discussion with a qualified healthcare professional.
            Return ONLY valid JSON when asked for JSON. Do not invent lab values, doses, or citations.
            """;

    public static String documentExtractionPrompt(String documentHint) {
        return SYSTEM_SAFETY + """
                
                Task: DocumentExtractionPrompt
                Analyze the medical document image/text. Document hint: %s
                
                Return JSON:
                {
                  "documentType": "LAB_REPORT|MEDICINE|PRESCRIPTION|DISCHARGE_SUMMARY|UNKNOWN",
                  "documentLabel": "short label",
                  "pages": 1,
                  "parameters": [
                    {
                      "name": "Hemoglobin",
                      "value": "10.2",
                      "unit": "g/dL",
                      "referenceRange": "12.0-15.0",
                      "status": "OUTSIDE_RANGE|WITHIN_RANGE|REQUIRES_DISCUSSION|IMPORTANT_ATTENTION|UNKNOWN|LOW_CONFIDENCE",
                      "explanation": "plain explanation without diagnosis",
                      "simpleExplanation": "explain like I am 12",
                      "confidence": 0.0
                    }
                  ],
                  "doctorQuestions": ["question1"],
                  "notes": ["optional"],
                  "rawTextSummary": "brief safe summary of visible content"
                }
                
                Rules:
                - Do not invent values. If unclear, set confidence < 0.6 and status LOW_CONFIDENCE, value null or "Unable to confidently read".
                - Status must NOT be a diagnosis label.
                """.formatted(documentHint == null ? "auto-detect" : documentHint);
    }

    public static String medicinePrompt() {
        return SYSTEM_SAFETY + """
                
                Task: MedicationExplanationPrompt
                Identify the medicine from the image if clearly visible.
                Return JSON:
                {
                  "name": "...",
                  "strength": "...",
                  "dosageForm": "...",
                  "manufacturer": "... or null",
                  "generalUse": "general informational use only",
                  "commonSideEffects": ["..."],
                  "precautions": ["..."],
                  "warnings": ["Follow the prescription provided by your healthcare professional."],
                  "confidence": 0.0
                }
                Never say "you should take this".
                """;
    }

    public static String prescriptionPrompt() {
        return SYSTEM_SAFETY + """
                
                Task: PrescriptionParsingPrompt
                Extract prescription lines only when confident.
                Return JSON:
                {
                  "items": [
                    {
                      "medicineName": "...",
                      "strength": "...",
                      "frequency": "1-0-1",
                      "timing": "morning/night",
                      "foodRelation": "After food",
                      "morning": true,
                      "afternoon": false,
                      "night": true,
                      "confident": true,
                      "note": null
                    }
                  ]
                }
                If handwriting unclear, set confident=false and note asking user to confirm with doctor/pharmacist.
                Do not invent missing dosage instructions.
                """;
    }

    public static String dischargePrompt() {
        return SYSTEM_SAFETY + """
                
                Task: DischargeSummaryPrompt
                Extract only what is present in the discharge document.
                Return JSON:
                {
                  "reasonForAdmission": "...",
                  "treatmentPerformed": "...",
                  "importantFindings": ["..."],
                  "medicinesListed": ["..."],
                  "followUpInstructions": ["..."],
                  "warningSigns": ["..."],
                  "doctorQuestions": ["..."]
                }
                """;
    }

    public static String translationPrompt(String targetLanguage, String text) {
        return SYSTEM_SAFETY + """
                
                Task: TranslationPrompt
                Translate the following medical explanation into %s.
                Preserve medical meaning. Prefer natural, understandable wording over literal translation.
                Keep Latin medical terms when helpful, then explain simply.
                
                Text:
                %s
                
                Return JSON: {"translated":"..."}
                """.formatted(targetLanguage, text);
    }

    public static String doctorQuestionPrompt(String context) {
        return SYSTEM_SAFETY + """
                
                Task: DoctorQuestionPrompt
                Based ONLY on this extracted document context, generate 5 useful non-diagnostic questions for a doctor visit.
                Context:
                %s
                Return JSON: {"questions":["..."]}
                """.formatted(context);
    }

    public static String voiceAssistantPrompt(String context, String query, String language) {
        return SYSTEM_SAFETY + """
                
                Task: VoiceAssistantPrompt
                Answer conversationally in language code %s using ONLY the document context when possible.
                Context:
                %s
                
                User query:
                %s
                
                Return JSON:
                {
                  "answer":"...",
                  "groundedFacts":["facts from document"],
                  "fromDocument": true
                }
                """.formatted(language, context, query);
    }

    public static String safetyReviewPrompt(String text) {
        return SYSTEM_SAFETY + """
                
                Task: SafetyReviewPrompt
                Rewrite the text to remove diagnosis claims and medication-change instructions while keeping meaning.
                Text:
                %s
                Return JSON: {"safeText":"...","notes":["..."]}
                """.formatted(text);
    }
}
