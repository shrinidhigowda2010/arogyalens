package com.arogyalens.ai;

import java.util.Optional;

/**
 * Port for generative AI used by the application services. {@link GeminiService} is the production
 * implementation; tests substitute a mock so no network calls are made.
 */
public interface AiClient {

    /** Whether AI is enabled and credentials are configured. */
    boolean isAvailable();

    /**
     * Generates a JSON answer for a text-only prompt.
     *
     * @throws com.arogyalens.exception.ArogyaLensException with an {@link AiErrors} code on failure
     */
    String generateJson(String prompt);

    /**
     * Generates a JSON answer for a prompt plus an uploaded image or PDF.
     *
     * @param what short description of the upload, used in user-facing error messages
     * @throws com.arogyalens.exception.ArogyaLensException with an {@link AiErrors} code on failure
     */
    String generateJson(String prompt, byte[] fileBytes, String mimeType, String what);

    /** Best-effort text generation; empty when AI is unavailable or the call fails. */
    Optional<String> generateText(String prompt);

    /** Text-to-speech; returns WAV audio. */
    byte[] synthesizeSpeech(String text);
}
