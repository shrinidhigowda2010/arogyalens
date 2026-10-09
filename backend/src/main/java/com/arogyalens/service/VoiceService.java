package com.arogyalens.service;

import com.arogyalens.ai.GeminiService;
import com.arogyalens.ai.PromptLibrary;
import com.arogyalens.dto.AnalysisResponse;
import com.arogyalens.dto.ChatRequest;
import com.arogyalens.dto.ChatResponse;
import com.arogyalens.dto.VoiceQueryRequest;
import com.arogyalens.dto.VoiceQueryResponse;
import com.arogyalens.model.MedicalParameter;
import com.arogyalens.model.TrustedSource;
import com.arogyalens.privacy.PrivacyService;
import com.arogyalens.safety.SafetyValidationService;
import com.arogyalens.source.SourceService;
import com.arogyalens.util.LanguageUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Ask-anything / voice assistant: grounded, multilingual, safety-checked answers. */
@Service
public class VoiceService {

    private final SessionService sessionService;
    private final GeminiService geminiService;
    private final SafetyValidationService safetyValidationService;
    private final LanguageUtil languageUtil;
    private final ObjectMapper objectMapper;
    private final PrivacyService privacyService;
    private final SourceService sourceService;

    public VoiceService(
            SessionService sessionService,
            GeminiService geminiService,
            SafetyValidationService safetyValidationService,
            LanguageUtil languageUtil,
            ObjectMapper objectMapper,
            PrivacyService privacyService,
            SourceService sourceService) {
        this.privacyService = privacyService;
        this.sourceService = sourceService;
        this.sessionService = sessionService;
        this.geminiService = geminiService;
        this.safetyValidationService = safetyValidationService;
        this.languageUtil = languageUtil;
        this.objectMapper = objectMapper;
    }

    /**
     * Answers a free-text health question in the requested language. Grounded in the session's
     * document when a valid {@code sessionId} is supplied, otherwise general information. The
     * question is PII-masked before it is sent to the AI, and the answer passes the safety layer.
     */
    public VoiceQueryResponse query(VoiceQueryRequest request) {
        String language = languageUtil.normalize(request.language());
        AnalysisResponse analysis = sessionService.get(request.sessionId()).orElse(null);
        String context = sessionService.context(request.sessionId());
        if (context.isBlank()) {
            context = "(No document uploaded. Give general, non-diagnostic health information.)";
        }
        String maskedQuery = privacyService.scanAndRedact(request.query()).redactedText();
        boolean emergency = safetyValidationService.isEmergency(request.query());
        List<TrustedSource> sources = sourceService.forTopic(request.query());

        Optional<String> ai =
                geminiService.generateText(
                        PromptLibrary.voiceAssistantPrompt(context, maskedQuery, language));

        if (ai.isPresent()) {
            try {
                JsonNode root = objectMapper.readTree(ai.get());
                String rawAnswer = root.path("answer").asText("");
                if (!rawAnswer.isBlank()) {
                    List<String> facts = new ArrayList<>();
                    root.path("groundedFacts").forEach(n -> facts.add(n.asText()));
                    var safety = safetyValidationService.validate(rawAnswer);
                    return new VoiceQueryResponse(
                            request.query(),
                            safety.sanitizedText(),
                            language,
                            facts,
                            safety.notes(),
                            analysis != null && root.path("fromDocument").asBoolean(true),
                            sources,
                            emergency);
                }
            } catch (Exception e) {
                // malformed AI JSON: use the grounded fallback below
            }
        }

        VoiceQueryResponse fallback = groundedFallback(request.query(), language, analysis);
        return new VoiceQueryResponse(
                fallback.query(),
                fallback.answer(),
                fallback.language(),
                fallback.groundedFacts(),
                fallback.safetyNotes(),
                fallback.fromDocument(),
                sources,
                emergency);
    }

    public ChatResponse chat(ChatRequest request) {
        VoiceQueryResponse voice =
                query(
                        new VoiceQueryRequest(
                                request.message(), request.sessionId(), request.language()));
        AnalysisResponse analysis = sessionService.get(request.sessionId()).orElse(null);
        List<String> fromDoc = new ArrayList<>();
        if (analysis != null && analysis.parameters() != null) {
            analysis.parameters().stream()
                    .filter(p -> p.status() != com.arogyalens.model.ParameterStatus.WITHIN_RANGE)
                    .limit(3)
                    .forEach(
                            p ->
                                    fromDoc.add(
                                            p.name()
                                                    + ": "
                                                    + p.value()
                                                    + (p.unit() == null ? "" : " " + p.unit())));
        }
        return new ChatResponse(
                voice.answer(),
                fromDoc,
                List.of(
                        "General health information should be confirmed with trusted sources and your clinician."),
                List.of("AI-generated explanation grounded in the uploaded document context."),
                voice.safetyNotes());
    }

    private VoiceQueryResponse groundedFallback(
            String query, String language, AnalysisResponse analysis) {
        String q = query.toLowerCase(Locale.ROOT);
        String answer;
        List<String> facts = new ArrayList<>();

        if (analysis == null) {
            answer =
                    "I couldn't answer that right now. Please try again in a moment, "
                            + "or upload a document so I can explain it, and discuss health questions with your healthcare professional.";
            var safety = safetyValidationService.validate(answer);
            return new VoiceQueryResponse(
                    query,
                    safety.sanitizedText(),
                    language,
                    facts,
                    safety.notes(),
                    false,
                    List.of(),
                    false);
        }

        MedicalParameter match = null;
        if (analysis.parameters() != null) {
            match =
                    analysis.parameters().stream()
                            .filter(
                                    p ->
                                            q.contains(p.name().toLowerCase(Locale.ROOT))
                                                    || (p.name().equalsIgnoreCase("HbA1c")
                                                            && q.contains("hba1c")))
                            .findFirst()
                            .orElse(null);
        }

        if (match != null) {
            facts.add(
                    match.name()
                            + " = "
                            + match.value()
                            + (match.unit() == null ? "" : " " + match.unit()));
            answer = match.explanation();
            if (!"en".equals(language)
                    && analysis.translations() != null
                    && analysis.translations().containsKey(language)
                    && match.name().equalsIgnoreCase("HbA1c")) {
                answer = analysis.translations().get(language);
            }
        } else if (q.contains("important") || q.contains("most")) {
            long outside =
                    analysis.parameters() == null
                            ? 0
                            : analysis.parameters().stream()
                                    .filter(
                                            p ->
                                                    p.status()
                                                            != com.arogyalens.model.ParameterStatus
                                                                    .WITHIN_RANGE)
                                    .count();
            answer =
                    "I found "
                            + outside
                            + " results that are outside the reference ranges shown on your report. "
                            + "I can explain each one, but these results alone do not establish a diagnosis.";
        } else {
            answer =
                    "I couldn't confidently determine this from the uploaded document. "
                            + "Try asking about a specific test name from your report, or discuss it with your healthcare professional.";
        }

        var safety = safetyValidationService.validate(answer);
        return new VoiceQueryResponse(
                query,
                safety.sanitizedText(),
                language,
                facts,
                safety.notes(),
                true,
                List.of(),
                false);
    }
}
