package com.arogyalens.model;

public record MedicalParameter(
        String name,
        String value,
        String unit,
        String referenceRange,
        ParameterStatus status,
        String explanation,
        String simpleExplanation,
        double confidence,
        boolean lowConfidence
) {
}
