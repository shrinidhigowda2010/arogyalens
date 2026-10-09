package com.arogyalens.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Text to run through the medical safety layer. */
public record SafetyValidateRequest(@NotBlank @Size(max = 8000) String text) {}
