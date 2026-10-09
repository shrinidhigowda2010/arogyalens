package com.arogyalens.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Chat message; {@code sessionId} is optional. */
public record ChatRequest(
        @Size(max = 64) String sessionId,
        @NotBlank @Size(max = 1000) String message,
        @Pattern(regexp = "^[a-z]{2}$") String language) {}
