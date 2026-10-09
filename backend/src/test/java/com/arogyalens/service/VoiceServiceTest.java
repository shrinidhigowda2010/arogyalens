package com.arogyalens.service;

import com.arogyalens.ai.GeminiService;
import com.arogyalens.dto.VoiceQueryRequest;
import com.arogyalens.dto.VoiceQueryResponse;
import com.arogyalens.privacy.PrivacyService;
import com.arogyalens.safety.SafetyValidationService;
import com.arogyalens.source.SourceService;
import com.arogyalens.support.TestProps;
import com.arogyalens.util.LanguageUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** VoiceService with Gemini mocked. */
class VoiceServiceTest {

    private final GeminiService gemini = mock(GeminiService.class);
    private final PrivacyService privacy = new PrivacyService();
    private final SessionService sessions = new SessionService(TestProps.defaults(), privacy);
    private final VoiceService voice = new VoiceService(sessions, gemini, new SafetyValidationService(),
            new LanguageUtil(TestProps.defaults()), new ObjectMapper(), privacy, new SourceService());

    @Test
    void answersGeneralQuestionWithoutSessionInRequestedLanguage() {
        when(gemini.generateText(anyString())).thenReturn(Optional.of(
                "{\"answer\":\"पैरासिटामोल बुखार में उपयोग होता है\",\"groundedFacts\":[],\"fromDocument\":true}"));

        VoiceQueryResponse res = voice.query(new VoiceQueryRequest("What is paracetamol used for?", null, "hi"));

        assertThat(res.language()).isEqualTo("hi");
        assertThat(res.answer()).contains("पैरासिटामोल");
        assertThat(res.fromDocument()).isFalse();
        assertThat(res.sources()).isNotEmpty();
    }

    @Test
    void masksPiiBeforeSendingQuestionToAi() {
        when(gemini.generateText(anyString())).thenReturn(Optional.empty());
        voice.query(new VoiceQueryRequest("My number is 9876543210, what is HbA1c?", null, "en"));

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(gemini).generateText(prompt.capture());
        assertThat(prompt.getValue()).doesNotContain("9876543210").contains("HbA1c").contains("English");
    }

    @Test
    void unsafeAiWordingIsRewritten() {
        when(gemini.generateText(anyString())).thenReturn(Optional.of(
                "{\"answer\":\"You have diabetes. Increase your dose.\"}"));
        VoiceQueryResponse res = voice.query(new VoiceQueryRequest("my sugar is high", null, "en"));
        assertThat(res.answer()).doesNotContain("You have diabetes").doesNotContain("Increase your dose");
        assertThat(res.safetyNotes()).isNotEmpty();
    }

    @Test
    void fallsBackGracefullyWhenAiUnavailableAndFlagsEmergency() {
        when(gemini.generateText(anyString())).thenReturn(Optional.empty());
        VoiceQueryResponse res = voice.query(new VoiceQueryRequest("I have severe chest pain", "", "en"));
        assertThat(res.answer()).isNotBlank();
        assertThat(res.emergency()).isTrue();
    }

    @Test
    void malformedAiJsonUsesFallback() {
        when(gemini.generateText(anyString())).thenReturn(Optional.of("not json"));
        VoiceQueryResponse res = voice.query(new VoiceQueryRequest("hello", null, "en"));
        assertThat(res.answer()).isNotBlank();
    }
}
