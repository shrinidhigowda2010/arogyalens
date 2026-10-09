package com.arogyalens.dto;

import java.util.List;

/** Chat answer split into document-grounded facts, general info and safety notes. */
public record ChatResponse(
        String answer,
        List<String> fromDocument,
        List<String> generalInfo,
        List<String> aiExplanation,
        List<String> safetyNotes) {}
