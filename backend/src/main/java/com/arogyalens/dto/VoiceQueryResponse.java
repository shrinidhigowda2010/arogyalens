package com.arogyalens.dto;

import java.util.List;

public record VoiceQueryResponse(
        String query,
        String answer,
        String language,
        List<String> groundedFacts,
        List<String> safetyNotes,
        boolean fromDocument
) {
}
