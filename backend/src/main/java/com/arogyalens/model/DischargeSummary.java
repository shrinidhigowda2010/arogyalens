package com.arogyalens.model;

import java.util.List;

/** Structured sections extracted from a discharge document. */
public record DischargeSummary(
        String reasonForAdmission,
        String treatmentPerformed,
        List<String> importantFindings,
        List<String> medicinesListed,
        List<String> followUpInstructions,
        List<String> warningSigns,
        List<String> doctorQuestions) {}
