package com.arogyalens.ai;

import com.arogyalens.config.ArogyaLensProperties;
import com.arogyalens.exception.ArogyaLensException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final ArogyaLensProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiService(ArogyaLensProperties properties,
                         RestClient.Builder restClientBuilder,
                         ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public boolean isAvailable() {
        return properties.ai().isConfigured();
    }

    public Optional<String> generateText(String prompt) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.2,
                            "responseMimeType", "application/json"
                    )
            );
            return Optional.ofNullable(callGemini(body));
        } catch (Exception e) {
            log.warn("Gemini text generation failed: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    public Optional<String> generateMultimodal(String prompt, byte[] fileBytes, String mimeType) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        try {
            String data = Base64.getEncoder().encodeToString(fileBytes);
            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mimeType", mimeType == null ? "image/jpeg" : mimeType);
            inlineData.put("data", data);

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt),
                                    Map.of("inlineData", inlineData)
                            ))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.2,
                            "responseMimeType", "application/json"
                    )
            );
            return Optional.ofNullable(callGemini(body));
        } catch (Exception e) {
            log.warn("Gemini multimodal generation failed: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private String callGemini(Map<String, Object> body) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + properties.ai().model()
                + ":generateContent?key=" + properties.ai().apiKey();

        try {
            String response = restClient
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                throw new ArogyaLensException(
                        "AI_EMPTY",
                        "Empty AI response",
                        "We couldn't confidently read this document. Try uploading a clearer image with the entire page visible."
                );
            }
            return extractJson(textNode.asText());
        } catch (ArogyaLensException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ArogyaLensException(
                    "AI_FAILURE",
                    "AI provider failure",
                    "The AI service is temporarily unavailable. Demo mode or retry may help."
            );
        }
    }

    public String extractJson(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}
