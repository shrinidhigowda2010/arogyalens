package com.arogyalens.service;

import com.arogyalens.ai.AiClient;
import com.arogyalens.ai.PromptLibrary;
import com.arogyalens.dto.AnalysisResponse;
import com.arogyalens.dto.ChatRequest;
import com.arogyalens.dto.ChatResponse;
import com.arogyalens.dto.VoiceQueryRequest;
import com.arogyalens.dto.VoiceQueryResponse;
import com.arogyalens.model.MedicalParameter;
import com.arogyalens.model.ParameterStatus;
import com.arogyalens.model.TrustedSource;
import com.arogyalens.privacy.PrivacyService;
import com.arogyalens.safety.SafetyValidationService;
import com.arogyalens.source.SourceService;
import com.arogyalens.util.LanguageUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Ask-anything / voice assistant: grounded, multilingual, safety-checked answers. */
@Service
public class VoiceService {

    private static final String NO_DOCUMENT_CONTEXT =
            "(No document uploaded. Give general, non-diagnostic health information.)";
    private static final String NO_ANSWER =
            "I couldn't answer that right now. Please try again in a moment, "
                    + "or upload a document so I can explain it, and discuss health questions with your healthcare professional.";
    private static final String NOT_IN_DOCUMENT =
            "I couldn't confidently determine this from the uploaded document. "
                    + "Try asking about a specific test name from your report, or discuss it with your healthcare professional.";
    private static final String HBA1C = "HbA1c";
    private static final int MAX_CHAT_FACTS = 3;

    private final SessionService sessionService;
    private final AiClient aiClient;
    private final SafetyValidationService safetyValidationService;
    private final LanguageUtil languageUtil;
    private final ObjectMapper objectMapper;
    private final PrivacyService privacyService;
    private final SourceService sourceService;

    public VoiceService(
            SessionService sessionService,
            AiClient aiClient,
            SafetyValidationService safetyValidationService,
            LanguageUtil languageUtil,
            ObjectMapper objectMapper,
            PrivacyService privacyService,
            SourceService sourceService) {
        this.privacyService = privacyService;
        this.sourceService = sourceService;
        this.sessionService = sessionService;
        this.aiClient = aiClient;
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
            context = NO_DOCUMENT_CONTEXT;
        }
        String maskedQuery = privacyService.scanAndRedact(request.query()).redactedText();
        boolean emergency = safetyValidationService.isEmergency(request.query());
        List<TrustedSource> sources = sourceService.forTopic(request.query());

        return aiClient.generateText(
                        PromptLibrary.voiceAssistantPrompt(context, maskedQuery, language))
                .flatMap(
                        json ->
                                fromAi(
                                        json,
                                        request.query(),
                                        language,
                                        analysis,
                                        sources,
                                        emergency))
                .orElseGet(
                        () ->
                                groundedFallback(
                                        request.query(), language, analysis, sources, emergency));
    }

    /** Chat variant of {@link #query}: adds the document's notable values to the answer. */
    public ChatResponse chat(ChatRequest request) {
        VoiceQueryResponse voice =
                query(
                        new VoiceQueryRequest(
                                request.message(), request.sessionId(), request.language()));
        AnalysisResponse analysis = sessionService.get(request.sessionId()).orElse(null);
        List<String> fromDoc = new ArrayList<>();
        if (analysis != null && analysis.parameters() != null) {
            analysis.parameters().stream()
                    .filter(p -> p.status() != ParameterStatus.WITHIN_RANGE)
                    .limit(MAX_CHAT_FACTS)
                    .forEach(p -> fromDoc.add(describe(p, ": ")));
        }
        return new ChatResponse(
                voice.answer(),
                fromDoc,
                List.of(
                        "General health information should be confirmed with trusted sources and your clinician."),
                List.of("AI-generated explanation grounded in the uploaded document context."),
                voice.safetyNotes());
    }

    private Optional<VoiceQueryResponse> fromAi(
            String json,
            String query,
            String language,
            AnalysisResponse analysis,
            List<TrustedSource> sources,
            boolean emergency) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String rawAnswer = root.path("answer").asText("");
            if (rawAnswer.isBlank()) {
                return Optional.empty();
            }
            List<String> facts = new ArrayList<>();
            root.path("groundedFacts").forEach(n -> facts.add(n.asText()));
            var safety = safetyValidationService.validate(rawAnswer);
            return Optional.of(
                    new VoiceQueryResponse(
                            query,
                            safety.sanitizedText(),
                            language,
                            facts,
                            safety.notes(),
                            analysis != null && root.path("fromDocument").asBoolean(true),
                            sources,
                            emergency));
        } catch (JsonProcessingException e) {
            return Optional.empty(); // malformed AI JSON: caller uses the grounded fallback
        }
    }

    /** Answer built without AI, from the session's document when there is one. */
    private VoiceQueryResponse groundedFallback(
            String query,
            String language,
            AnalysisResponse analysis,
            List<TrustedSource> sources,
            boolean emergency) {
        List<String> facts = new ArrayList<>();
        String answer =
                analysis == null
                        ? NO_ANSWER
                        : answerFromDocument(
                                query.toLowerCase(Locale.ROOT), language, analysis, facts);
        var safety = safetyValidationService.validate(answer);
        return new VoiceQueryResponse(
                query,
                safety.sanitizedText(),
                language,
                facts,
                safety.notes(),
                analysis != null,
                sources,
                emergency);
    }

    private String answerFromDocument(
            String q, String language, AnalysisResponse analysis, List<String> facts) {
        List<MedicalParameter> parameters =
                analysis.parameters() == null ? List.of() : analysis.parameters();
        Optional<MedicalParameter> match =
                parameters.stream().filter(p -> mentions(q, p)).findFirst();
        if (match.isPresent()) {
            MedicalParameter p = match.get();
            facts.add(describe(p, " = "));
            Map<String, String> translations = analysis.translations();
            boolean translatedHba1c =
                    !"en".equals(language)
                            && translations != null
                            && translations.containsKey(language)
                            && p.name().equalsIgnoreCase(HBA1C);
            return translatedHba1c ? translations.get(language) : p.explanation();
        }
        if (q.contains("important") || q.contains("most")) {
            long outside =
                    parameters.stream()
                            .filter(p -> p.status() != ParameterStatus.WITHIN_RANGE)
                            .count();
            return "I found "
                    + outside
                    + " results that are outside the reference ranges shown on your report. "
                    + "I can explain each one, but these results alone do not establish a diagnosis.";
        }
        return NOT_IN_DOCUMENT;
    }

    private static boolean mentions(String lowerQuery, MedicalParameter p) {
        return lowerQuery.contains(p.name().toLowerCase(Locale.ROOT))
                || (p.name().equalsIgnoreCase(HBA1C) && lowerQuery.contains("hba1c"));
    }

    private static String describe(MedicalParameter p, String separator) {
        return p.name() + separator + p.value() + (p.unit() == null ? "" : " " + p.unit());
    }
}
