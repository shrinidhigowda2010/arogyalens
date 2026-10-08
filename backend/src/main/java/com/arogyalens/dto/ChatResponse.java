package com.arogyalens.dto;

import java.util.List;

public record ChatResponse(
        String answer,
        List<String> fromDocument,
        List<String> generalInfo,
        List<String> aiExplanation,
        List<String> safetyNotes
) {
}
