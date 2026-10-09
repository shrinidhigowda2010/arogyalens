package com.arogyalens.model;

import java.util.List;

public record DischargeSummary(
        String reasonForAdmission,
        String treatmentPerformed,
        List<String> importantFindings,
        List<String> medicinesListed,
        List<String> followUpInstructions,
        List<String> warningSigns,
        List<String> doctorQuestions) {}
