package com.arogyalens.ai;

import com.arogyalens.config.ArogyaLensProperties;
import com.arogyalens.exception.ArogyaLensException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
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
    private final List<String> fallbackModels;

    public GeminiService(ArogyaLensProperties properties,
                         RestClient.Builder restClientBuilder,
                         ObjectMapper objectMapper,
                         Environment environment) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        String raw = environment.getProperty("arogyalens.ai.fallback-models",
                "gemini-flash-latest,gemini-flash-lite-latest");
        List<String> parsed = new ArrayList<>();
        for (String m : raw.split(",")) {
            if (!m.isBlank()) {
                parsed.add(m.strip());
            }
        }
        this.fallbackModels = List.copyOf(parsed);
    }

    public boolean isAvailable() {
        return properties.ai().isConfigured() && !apiKey().isEmpty();
    }

    /**
     * API key as configured, with surrounding whitespace/newlines and quotes removed.
     * Values pasted into hosting dashboards (e.g. Render) often carry a trailing space,
     * newline or quotes, which Google rejects (400 API_KEY_INVALID / 401 UNAUTHENTICATED).
     */
    private String apiKey() {
        String key = properties.ai().apiKey();
        if (key == null) {
            return "";
        }
        key = key.strip();
        if (key.length() >= 2 && ((key.startsWith("\"") && key.endsWith("\""))
                || (key.startsWith("'") && key.endsWith("'")))) {
            key = key.substring(1, key.length() - 1).strip();
        }
        return key;
    }

    private String model() {
        String model = properties.ai().model();
        return model == null ? "" : model.strip();
    }

    private String redact(String text) {
        if (text == null) {
            return "";
        }
        String key = apiKey();
        String out = key.isEmpty() ? text : text.replace(key, "***");
        String raw = properties.ai().apiKey();
        if (raw != null && !raw.isBlank()) {
            out = out.replace(raw, "***");
        }
        return out;
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
            log.warn("Gemini text generation failed: {}", describe(e));
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
            log.warn("Gemini multimodal generation failed: {}", describe(e));
            return Optional.empty();
        }
    }

    private String describe(Exception e) {
        if (e instanceof ArogyaLensException ale) {
            return ale.getCode() + " " + redact(ale.getMessage());
        }
        return e.getClass().getSimpleName() + " " + redact(e.getMessage());
    }

    /**
     * Calls the primary model; on 429 (quota), 404 (model retired), 5xx (overloaded) or a
     * timeout/network error, falls back to each model in arogyalens.ai.fallback-models in order.
     * Other errors (e.g. 400 invalid key, 403) fail immediately since another model won't help.
     */
    private String callGemini(Map<String, Object> body) {
        LinkedHashSet<String> models = new LinkedHashSet<>();
        models.add(model());
        models.addAll(fallbackModels);
        ArogyaLensException last = null;
        for (String model : models) {
            if (model.isEmpty()) {
                continue;
            }
            for (int attempt = 1; attempt <= 2; attempt++) {
                try {
                    return callGeminiOnce(body, model);
                } catch (ArogyaLensException ex) {
                    last = ex;
                    String msg = ex.getMessage() == null ? "" : ex.getMessage();
                    boolean serverError = msg.matches(".*HTTP 5\\d\\d$");
                    boolean switchModel = serverError
                            || msg.endsWith("HTTP 429")
                            || msg.endsWith("HTTP 404")
                            || "AI provider failure".equals(msg); // timeout / network error
                    if (!switchModel) {
                        throw ex;
                    }
                    if (serverError && attempt == 1) {
                        sleepQuietly(700L); // one quick retry on the same model for "high demand"
                        continue;
                    }
                    log.warn("Gemini model {} unavailable ({}); trying next fallback model", model, msg);
                    break;
                }
            }
        }
        throw last != null ? last : new ArogyaLensException(
                "AI_FAILURE", "AI provider failure: no model configured",
                "The AI service is temporarily unavailable. Demo mode or retry may help.");
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private String callGeminiOnce(Map<String, Object> body, String model) {
        try {
            // Key goes in the x-goog-api-key header (not the URL) so it is never URI-encoded or logged.
            String response = restClient
                    .post()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
            JsonNode candidate = root.path("candidates").path(0);
            StringBuilder text = new StringBuilder();
            for (JsonNode part : candidate.path("content").path("parts")) {
                if (part.path("thought").asBoolean(false)) {
                    continue; // skip thinking summaries
                }
                JsonNode t = part.path("text");
                if (t.isTextual()) {
                    text.append(t.asText());
                }
            }
            if (text.toString().isBlank()) {
                log.warn("Gemini returned no text (model={}, finishReason={}, blockReason={})",
                        model,
                        candidate.path("finishReason").asText("n/a"),
                        root.path("promptFeedback").path("blockReason").asText("n/a"));
                throw new ArogyaLensException(
                        "AI_EMPTY",
                        "Empty AI response",
                        "We couldn't confidently read this document. Try uploading a clearer image with the entire page visible."
                );
            }
            return extractJson(text.toString());
        } catch (ArogyaLensException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            String errorBody = redact(ex.getResponseBodyAsString()).replaceAll("\\s+", " ");
            if (errorBody.length() > 800) {
                errorBody = errorBody.substring(0, 800) + "...";
            }
            log.warn("Gemini HTTP {} (model={}): {}", ex.getStatusCode().value(), model, errorBody);
            throw new ArogyaLensException(
                    "AI_FAILURE",
                    "AI provider failure: HTTP " + ex.getStatusCode().value(),
                    "The AI service is temporarily unavailable. Demo mode or retry may help."
            );
        } catch (Exception ex) {
            log.warn("Gemini call error (model={}): {} {}", model, ex.getClass().getName(), redact(ex.getMessage()));
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
