package com.arogyalens.dto;

import java.util.Map;

/** Translated text for a target language. */
public record TranslateResponse(
        String original,
        String targetLanguage,
        String translated,
        Map<String, String> allLanguages,
        String medicalTerm,
        String simpleExplanation) {}
