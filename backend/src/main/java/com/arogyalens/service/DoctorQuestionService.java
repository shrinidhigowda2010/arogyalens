package com.arogyalens.service;

import com.arogyalens.ai.AiClient;
import com.arogyalens.ai.PromptLibrary;
import com.arogyalens.dto.AnalysisResponse;
import com.arogyalens.model.MedicalParameter;
import com.arogyalens.safety.SafetyValidationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Builds grounded questions for a doctor visit from a stored session. */
@Service
public class DoctorQuestionService {

    private final SessionService sessionService;
    private final AiClient aiClient;
    private final SafetyValidationService safetyValidationService;
    private final ObjectMapper objectMapper;

    public DoctorQuestionService(
            SessionService sessionService,
            AiClient aiClient,
            SafetyValidationService safetyValidationService,
            ObjectMapper objectMapper) {
        this.sessionService = sessionService;
        this.aiClient = aiClient;
        this.safetyValidationService = safetyValidationService;
        this.objectMapper = objectMapper;
    }

    public List<String> forSession(String sessionId) {
        AnalysisResponse response = sessionService.require(sessionId);
        if (response.doctorQuestions() != null && !response.doctorQuestions().isEmpty()) {
            return response.doctorQuestions();
        }
        String context = sessionService.context(sessionId);
        return aiClient.generateText(PromptLibrary.doctorQuestionPrompt(context))
                .map(this::parseQuestions)
                .orElse(defaultQuestions());
    }

    public List<String> fromJsonOrDefault(JsonNode node, List<MedicalParameter> parameters) {
        List<String> questions = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(
                    n -> questions.add(safetyValidationService.enforceSafeWording(n.asText())));
        }
        if (!questions.isEmpty()) {
            return questions;
        }
        if (parameters != null
                && parameters.stream()
                        .anyMatch(
                                p ->
                                        p.status()
                                                        == com.arogyalens.model.ParameterStatus
                                                                .OUTSIDE_RANGE
                                                || p.status()
                                                        == com.arogyalens.model.ParameterStatus
                                                                .REQUIRES_DISCUSSION
                                                || p.status()
                                                        == com.arogyalens.model.ParameterStatus
                                                                .IMPORTANT_ATTENTION)) {
            return defaultQuestions();
        }
        return defaultQuestions();
    }

    private List<String> parseQuestions(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<String> list = new ArrayList<>();
            root.path("questions")
                    .forEach(n -> list.add(safetyValidationService.enforceSafeWording(n.asText())));
            return list.isEmpty() ? defaultQuestions() : list;
        } catch (JsonProcessingException e) {
            // malformed AI JSON: use the curated default questions
            return defaultQuestions();
        }
    }

    private List<String> defaultQuestions() {
        return List.of(
                "What could explain this result?",
                "Does this result need to be repeated?",
                "Do I need any additional tests?",
                "Could any of my current medicines affect this result?",
                "When should I follow up?");
    }
}
