package com.arogyalens.ai;

import com.arogyalens.config.ArogyaLensProperties;
import com.arogyalens.exception.ArogyaLensException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Thin client for the Google Gemini API ({@code generateContent}).
 *
 * <ul>
 *   <li>Authenticates with the {@code x-goog-api-key} header; the key is never placed in URLs or
 *       logs.
 *   <li>Tries the primary model, then each fallback model, on quota (429), retired model (404),
 *       overload (5xx, retried once with backoff) or timeout.
 *   <li>Maps failures to distinct user-facing errors (see {@link AiErrors}).
 *   <li>Caches identical requests briefly via {@link AiResponseCache}.
 * </ul>
 */
@Service
public class GeminiService implements AiClient {

    static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent";
    private static final Logger LOG = LoggerFactory.getLogger(GeminiService.class);
    private static final int TTS_SAMPLE_RATE = 24_000;

    private final ArogyaLensProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AiResponseCache cache;
    private LongConsumer sleeper = GeminiService::sleepQuietly;

    public GeminiService(
            ArogyaLensProperties properties,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            AiResponseCache cache) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.cache = cache;
    }

    /** Replaces the backoff sleeper (used by tests to avoid real waiting). */
    void setSleeper(LongConsumer sleeper) {
        this.sleeper = sleeper;
    }

    /**
     * @return true when AI is enabled and a non-blank API key is configured.
     */
    @Override
    public boolean isAvailable() {
        return properties.ai().enabled() && !cleanKey(properties.ai().apiKey()).isEmpty();
    }

    /**
     * Normalises an API key pasted into a hosting dashboard: strips surrounding whitespace,
     * newlines and matching quotes, which Google would otherwise reject.
     */
    public static String cleanKey(String raw) {
        if (raw == null) {
            return "";
        }
        String key = raw.strip();
        if (key.length() >= 2
                && ((key.startsWith("\"") && key.endsWith("\""))
                        || (key.startsWith("'") && key.endsWith("'")))) {
            key = key.substring(1, key.length() - 1).strip();
        }
        return key;
    }

    /** Ordered, de-duplicated list of models to try: primary first, then fallbacks. */
    List<String> modelChain() {
        LinkedHashSet<String> models = new LinkedHashSet<>();
        String primary = properties.ai().model() == null ? "" : properties.ai().model().strip();
        if (!primary.isEmpty()) {
            models.add(primary);
        }
        models.addAll(properties.ai().fallbackModelList());
        return List.copyOf(models);
    }

    /**
     * Generates a JSON answer for a text prompt.
     *
     * @throws ArogyaLensException with a code from {@link AiErrors} on failure
     */
    @Override
    public String generateJson(String prompt) {
        requireAvailable();
        Map<String, Object> body =
                Map.of(
                        "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                        "generationConfig", jsonConfig());
        return cached(AiResponseCache.key("text", prompt), body, "answer");
    }

    /**
     * Generates a JSON answer for a prompt plus an inline image or PDF.
     *
     * @throws ArogyaLensException with a code from {@link AiErrors} on failure
     */
    @Override
    public String generateJson(String prompt, byte[] fileBytes, String mimeType, String what) {
        requireAvailable();
        String data = Base64.getEncoder().encodeToString(fileBytes);
        String mime = mimeType == null || mimeType.isBlank() ? "image/jpeg" : mimeType;
        Map<String, Object> body =
                Map.of(
                        "contents",
                                List.of(
                                        Map.of(
                                                "parts",
                                                List.of(
                                                        Map.of("text", prompt),
                                                        Map.of(
                                                                "inlineData",
                                                                Map.of(
                                                                        "mimeType",
                                                                        mime,
                                                                        "data",
                                                                        data))))),
                        "generationConfig", jsonConfig());
        return cached(AiResponseCache.key("file", prompt, mime, data), body, what);
    }

    /**
     * Best-effort text generation that returns empty instead of throwing (for optional
     * enrichments).
     */
    @Override
    public Optional<String> generateText(String prompt) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        try {
            return Optional.of(generateJson(prompt));
        } catch (ArogyaLensException e) {
            LOG.warn("Optional Gemini text generation failed: {}", e.getCode());
            return Optional.empty();
        }
    }

    /**
     * Synthesises speech with Gemini TTS and returns a WAV file (16-bit mono PCM, 24 kHz). Used
     * only when the user's device has no voice for the selected language.
     */
    @Override
    public byte[] synthesizeSpeech(String text) {
        requireAvailable();
        Map<String, Object> body =
                Map.of(
                        "contents", List.of(Map.of("parts", List.of(Map.of("text", text)))),
                        "generationConfig",
                                Map.of(
                                        "responseModalities", List.of("AUDIO"),
                                        "speechConfig",
                                                Map.of(
                                                        "voiceConfig",
                                                        Map.of(
                                                                "prebuiltVoiceConfig",
                                                                Map.of("voiceName", "Kore")))));
        String model = properties.ai().ttsModel() == null ? "" : properties.ai().ttsModel().strip();
        if (model.isEmpty()) {
            throw AiErrors.failure();
        }
        JsonNode root;
        try {
            root = post(model, body);
        } catch (CallFailure failure) {
            throw failure.toUserError("audio");
        }
        for (JsonNode part : root.path("candidates").path(0).path("content").path("parts")) {
            String b64 = part.path("inlineData").path("data").asText("");
            if (!b64.isEmpty()) {
                return pcmToWav(Base64.getDecoder().decode(b64), TTS_SAMPLE_RATE);
            }
        }
        throw AiErrors.failure();
    }

    private String cached(String key, Map<String, Object> body, String what) {
        Optional<String> hit = cache.get(key);
        if (hit.isPresent()) {
            return hit.get();
        }
        String result = callWithFallback(body, what);
        cache.put(key, result);
        return result;
    }

    private void requireAvailable() {
        if (!isAvailable()) {
            throw AiErrors.notConfigured();
        }
    }

    private Map<String, Object> jsonConfig() {
        return Map.of("temperature", 0.2, "responseMimeType", "application/json");
    }

    private String callWithFallback(Map<String, Object> body, String what) {
        List<String> models = modelChain();
        if (models.isEmpty()) {
            throw AiErrors.notConfigured();
        }
        CallFailure last = null;
        for (String model : models) {
            for (int attempt = 1; attempt <= 2; attempt++) {
                try {
                    return extractText(post(model, body), model, what);
                } catch (CallFailure failure) {
                    last = failure;
                    if (!failure.kind.tryNextModel) {
                        throw failure.toUserError(what);
                    }
                    if (failure.kind == Kind.BUSY && attempt == 1) {
                        sleeper.accept(700L);
                        continue;
                    }
                    LOG.warn(
                            "Gemini model {} unavailable ({}); trying next model",
                            model,
                            failure.kind);
                    break;
                }
            }
        }
        throw last.toUserError(what);
    }

    private JsonNode post(String model, Map<String, Object> body) {
        try {
            String response =
                    restClient
                            .post()
                            .uri(BASE_URL, model)
                            .header("x-goog-api-key", cleanKey(properties.ai().apiKey()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(body)
                            .retrieve()
                            .body(String.class);
            return objectMapper.readTree(response == null ? "{}" : response);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            String errorBody = redact(ex.getResponseBodyAsString()).replaceAll("\\s+", " ");
            LOG.warn(
                    "Gemini HTTP {} (model={}): {}",
                    status,
                    model,
                    errorBody.length() > 600 ? errorBody.substring(0, 600) + "..." : errorBody);
            throw new CallFailure(classify(status, errorBody));
        } catch (ResourceAccessException ex) {
            LOG.warn("Gemini network error (model={}): {}", model, redact(ex.getMessage()));
            throw new CallFailure(Kind.TIMEOUT);
        } catch (CallFailure ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.warn("Gemini call error (model={}): {}", model, ex.getClass().getSimpleName());
            throw new CallFailure(Kind.FAILURE);
        }
    }

    static Kind classify(int status, String body) {
        String b = body == null ? "" : body;
        if (status == 401 || status == 403 || b.contains("API_KEY_INVALID")) {
            return Kind.KEY_INVALID;
        }
        if (status == 429) {
            return Kind.QUOTA;
        }
        if (status == 404) {
            return Kind.MODEL_MISSING;
        }
        if (status >= 500) {
            return Kind.BUSY;
        }
        if (status == 400) {
            return Kind.BAD_INPUT;
        }
        return Kind.FAILURE;
    }

    private String extractText(JsonNode root, String model, String what) {
        JsonNode candidate = root.path("candidates").path(0);
        StringBuilder text = new StringBuilder();
        for (JsonNode part : candidate.path("content").path("parts")) {
            if (part.path("thought").asBoolean(false)) {
                continue;
            }
            JsonNode t = part.path("text");
            if (t.isTextual()) {
                text.append(t.asText());
            }
        }
        if (text.toString().isBlank()) {
            LOG.warn(
                    "Gemini returned no text (model={}, finishReason={}, blockReason={})",
                    model,
                    candidate.path("finishReason").asText("n/a"),
                    root.path("promptFeedback").path("blockReason").asText("n/a"));
            throw new CallFailure(Kind.EMPTY);
        }
        return extractJson(text.toString());
    }

    /** Strips Markdown code fences and surrounding prose, returning the outermost JSON object. */
    public String extractJson(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
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

    private String redact(String text) {
        if (text == null) {
            return "";
        }
        String key = cleanKey(properties.ai().apiKey());
        return key.isEmpty() ? text : text.replace(key, "***");
    }

    static byte[] pcmToWav(byte[] pcm, int sampleRate) {
        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        int byteRate = sampleRate * 2;
        header.put("RIFF".getBytes())
                .putInt(36 + pcm.length)
                .put("WAVE".getBytes())
                .put("fmt ".getBytes())
                .putInt(16)
                .putShort((short) 1)
                .putShort((short) 1)
                .putInt(sampleRate)
                .putInt(byteRate)
                .putShort((short) 2)
                .putShort((short) 16)
                .put("data".getBytes())
                .putInt(pcm.length);
        ByteArrayOutputStream out = new ByteArrayOutputStream(44 + pcm.length);
        out.writeBytes(header.array());
        out.writeBytes(pcm);
        return out.toByteArray();
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /** Failure categories for one Gemini call; {@code tryNextModel} drives model fallback. */
    enum Kind {
        QUOTA(true),
        MODEL_MISSING(true),
        BUSY(true),
        TIMEOUT(true),
        KEY_INVALID(false),
        BAD_INPUT(false),
        EMPTY(true),
        FAILURE(false);

        final boolean tryNextModel;

        Kind(boolean tryNextModel) {
            this.tryNextModel = tryNextModel;
        }
    }

    private static final class CallFailure extends RuntimeException {
        private final Kind kind;

        CallFailure(Kind kind) {
            super(kind.name(), null, false, false);
            this.kind = kind;
        }

        ArogyaLensException toUserError(String what) {
            return switch (kind) {
                case QUOTA -> AiErrors.quota();
                case BUSY -> AiErrors.busy();
                case TIMEOUT -> AiErrors.timeout();
                case KEY_INVALID -> AiErrors.keyInvalid();
                case BAD_INPUT, EMPTY -> AiErrors.unreadable(what);
                case MODEL_MISSING, FAILURE -> AiErrors.failure();
            };
        }
    }
}
