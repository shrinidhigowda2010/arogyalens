package com.arogyalens.dto;

import jakarta.validation.constraints.NotBlank;

public record VoiceQueryRequest(
        @NotBlank String query,
        String sessionId,
        String language
) {
}
