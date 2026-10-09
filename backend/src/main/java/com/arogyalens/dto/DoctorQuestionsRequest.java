package com.arogyalens.dto;

import jakarta.validation.constraints.NotBlank;

public record DoctorQuestionsRequest(@NotBlank String sessionId, String language) {}
