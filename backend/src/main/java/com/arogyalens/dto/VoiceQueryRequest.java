package com.arogyalens.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Question for the assistant; {@code sessionId} is optional (general health Q&A without a
 * document).
 */
public record VoiceQueryRequest(
        @NotBlank @Size(max = 1000) String query,
        @Size(max = 64) String sessionId,
        @Pattern(regexp = "^[a-z]{2}$") String language) {}
