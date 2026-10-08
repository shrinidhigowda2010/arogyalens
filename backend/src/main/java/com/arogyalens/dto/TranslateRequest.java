package com.arogyalens.dto;

import jakarta.validation.constraints.NotBlank;

public record TranslateRequest(
        @NotBlank String text,
        @NotBlank String targetLanguage,
        String medicalTerm,
        String context
) {
}
