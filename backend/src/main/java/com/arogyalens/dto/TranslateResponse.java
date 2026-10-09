package com.arogyalens.dto;

import java.util.Map;

public record TranslateResponse(
        String original,
        String targetLanguage,
        String translated,
        Map<String, String> allLanguages,
        String medicalTerm,
        String simpleExplanation) {}
