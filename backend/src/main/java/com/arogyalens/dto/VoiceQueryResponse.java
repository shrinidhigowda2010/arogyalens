package com.arogyalens.dto;

import com.arogyalens.model.TrustedSource;

import java.util.List;

/** Answer from the ask/voice assistant, already passed through the safety layer. */
public record VoiceQueryResponse(
        String query,
        String answer,
        String language,
        List<String> groundedFacts,
        List<String> safetyNotes,
        boolean fromDocument,
        List<TrustedSource> sources,
        boolean emergency
) {
}
