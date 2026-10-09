package com.arogyalens.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Text to translate into one of the supported Indian languages. */
public record TranslateRequest(
        @NotBlank @Size(max = 4000) String text,
        @NotBlank @Pattern(regexp = "^[a-z]{2}$") String targetLanguage,
        @Size(max = 200) String medicalTerm,
        @Size(max = 2000) String context) {}
