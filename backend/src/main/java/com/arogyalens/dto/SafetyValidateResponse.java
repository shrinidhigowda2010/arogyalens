package com.arogyalens.dto;

import java.util.List;

public record SafetyValidateResponse(
        boolean safe, String sanitizedText, List<String> violations, List<String> notes) {}
