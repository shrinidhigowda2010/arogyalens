package com.arogyalens.service;

import com.arogyalens.ai.AiClient;
import com.arogyalens.exception.ArogyaLensException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Speech-to-text for browsers without the Web Speech API (e.g. Firefox): a short recorded clip is
 * sent to Gemini as inline audio and transcribed in the chosen language.
 */
@Service
public class TranscriptionService {

    /** Largest accepted clip (about 15 seconds of compressed speech is far smaller). */
    public static final long MAX_AUDIO_BYTES = 2L * 1024 * 1024;

    /** Audio formats Gemini accepts that browsers record. */
    public static final Set<String> ALLOWED_TYPES =
            Set.of("audio/webm", "audio/ogg", "audio/wav", "audio/mpeg", "audio/mp4");

    private static final String RETRY = "We could not hear that clearly. Please try again or type.";

    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    public TranscriptionService(AiClient aiClient, ObjectMapper objectMapper) {
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Transcribes the clip exactly as spoken.
     *
     * @return the transcript, possibly empty when nothing was said
     */
    public String transcribe(MultipartFile audio, String language) {
        String mime = baseType(audio);
        byte[] bytes;
        try {
            bytes = audio.getBytes();
        } catch (IOException e) {
            throw badAudio("Unreadable audio upload");
        }
        String prompt =
                "Transcribe this audio exactly as spoken. The speaker is using language code '"
                        + safeLanguage(language)
                        + "'. Write the words in that language's usual script and do not"
                        + " translate or answer. Return only JSON {\"text\": \"...\"}; use an"
                        + " empty string if there is no speech.";
        String raw = aiClient.generateJson(prompt, bytes, mime, "Voice");
        try {
            return objectMapper.readTree(raw).path("text").asText("").trim();
        } catch (JsonProcessingException e) {
            throw ScanSupport.analysisFailed("Voice", RETRY);
        }
    }

    private static String baseType(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw badAudio("Empty audio upload");
        }
        if (audio.getSize() > MAX_AUDIO_BYTES) {
            throw new ArogyaLensException(
                    "FILE_TOO_LARGE",
                    "Audio too large",
                    "That recording is too long. Please keep it under 15 seconds.",
                    HttpStatus.PAYLOAD_TOO_LARGE);
        }
        String type = audio.getContentType();
        String base = type == null ? "" : type.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(base)) {
            throw new ArogyaLensException(
                    "UNSUPPORTED_TYPE",
                    "Unsupported audio type " + base,
                    "This audio format is not supported. Please type your question instead.",
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
        return base;
    }

    private static String safeLanguage(String language) {
        return language != null && language.matches("[a-z]{2}") ? language : "en";
    }

    private static ArogyaLensException badAudio(String message) {
        return new ArogyaLensException("EMPTY_FILE", message, RETRY, HttpStatus.BAD_REQUEST);
    }
}
