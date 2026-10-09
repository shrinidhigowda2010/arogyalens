package com.arogyalens.dto;

import java.util.List;

/** Result of running text through the safety layer. */
public record SafetyValidateResponse(
        boolean safe, String sanitizedText, List<String> violations, List<String> notes) {}
