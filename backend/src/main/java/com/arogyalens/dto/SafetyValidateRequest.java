package com.arogyalens.dto;

import jakarta.validation.constraints.NotBlank;

public record SafetyValidateRequest(@NotBlank String text) {}
