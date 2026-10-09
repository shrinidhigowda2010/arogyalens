package com.arogyalens.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PromptTemplatesTest {

    @Test
    void everyTemplateLoadsAndIsNotBlank() {
        for (String name : PromptTemplates.NAMES) {
            assertThat(PromptTemplates.raw(name)).as(name).isNotBlank();
        }
    }

    @Test
    void everyBuiltPromptHasSafetyRulesAndNoUnfilledPlaceholders() {
        List<String> prompts =
                List.of(
                        PromptLibrary.documentExtractionPrompt(null),
                        PromptLibrary.medicinePrompt(),
                        PromptLibrary.prescriptionPrompt(),
                        PromptLibrary.dischargePrompt(),
                        PromptLibrary.translationPrompt("Hindi (hi)", "Take rest"),
                        PromptLibrary.doctorQuestionPrompt("HbA1c 7.8"),
                        PromptLibrary.voiceAssistantPrompt("ctx", "what is fever", "kn"),
                        PromptLibrary.specialtyPrompt("chest pain", "ta"),
                        PromptLibrary.safetyReviewPrompt("You have diabetes"));
        for (String prompt : prompts) {
            assertThat(prompt).startsWith(PromptLibrary.SYSTEM_SAFETY).doesNotContain("{{");
        }
        assertThat(PromptLibrary.documentExtractionPrompt(null)).contains("auto-detect");
        assertThat(PromptLibrary.voiceAssistantPrompt("ctx", "q", "kn")).contains("Kannada (kn)");
        assertThat(PromptLibrary.specialtyPrompt("x", "zz")).contains("in English");
    }

    @Test
    void userTextIsNeverTreatedAsAPlaceholder() {
        String prompt =
                PromptLibrary.voiceAssistantPrompt("SECRET-CONTEXT", "{{context}} $1 \\", "en");
        assertThat(prompt).contains("<question>\n{{context}} $1 \\\n</question>");
        assertThat(prompt.indexOf("SECRET-CONTEXT"))
                .isEqualTo(prompt.lastIndexOf("SECRET-CONTEXT"));
    }

    @Test
    void placeholdersAreDiscoverableAndRequired() {
        assertThat(PromptTemplates.placeholders("voice-assistant"))
                .containsExactly("context", "language", "question");
        assertThatThrownBy(() -> PromptTemplates.render("translation", Map.of("text", "x")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("{{language}}");
        assertThatThrownBy(() -> PromptTemplates.raw("nope"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
