package com.arogyalens.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request for doctor questions grounded in a scanned document session. */
public record DoctorQuestionsRequest(
        @NotBlank @Size(max = 64) String sessionId,
        @Pattern(regexp = "^[a-z]{2}$") String language) {}
