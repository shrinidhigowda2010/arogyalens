package com.arogyalens.model;

/** One lab value with its unit, reference range, status and plain-language explanation. */
public record MedicalParameter(
        String name,
        String value,
        String unit,
        String referenceRange,
        ParameterStatus status,
        String explanation,
        String simpleExplanation,
        double confidence,
        boolean lowConfidence) {}
