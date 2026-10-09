package com.arogyalens.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Edge cases: empty/oversized/unsupported input, unknown resources and AI-off behaviour. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiEdgeCaseTest {

    @Autowired MockMvc mockMvc;

    private org.springframework.test.web.servlet.ResultActions postJson(String path, String body)
            throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void dischargeDemoAnalysis() throws Exception {
        mockMvc.perform(multipart("/api/discharge/analyze").param("demo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dischargeSummary").exists());
    }

    @Test
    void translateValidatesLanguageAndLength() throws Exception {
        postJson("/api/translate", "{\"text\":\"Take rest\",\"targetLanguage\":\"hi\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetLanguage").value("hi"));
        postJson("/api/translate", "{\"text\":\"x\",\"targetLanguage\":\"hindi!\"}")
                .andExpect(status().isBadRequest());
        postJson(
                        "/api/translate",
                        "{\"text\":\"" + "a".repeat(4001) + "\",\"targetLanguage\":\"hi\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void emptyAndOversizedQuestionsAreRejected() throws Exception {
        postJson("/api/voice/query", "{\"query\":\"   \",\"language\":\"en\"}")
                .andExpect(status().isBadRequest());
        postJson("/api/chat", "{\"message\":\"" + "a".repeat(1001) + "\",\"language\":\"en\"}")
                .andExpect(status().isBadRequest());
        postJson("/api/safety/validate", "{\"text\":\"\"}").andExpect(status().isBadRequest());
    }

    @Test
    void unsupportedLanguageIsRejectedWithFriendlyError() throws Exception {
        postJson("/api/voice/query", "{\"query\":\"what is fever\",\"language\":\"zz\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void doctorQuestionsForUnknownSessionIs404() throws Exception {
        postJson("/api/doctor-questions", "{\"sessionId\":\"nope\",\"language\":\"en\"}")
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void sourcesListAndUnknownId() throws Exception {
        mockMvc.perform(get("/api/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].url").exists());
        mockMvc.perform(get("/api/sources/does-not-exist")).andExpect(status().isNotFound());
    }

    @Test
    void ttsWithoutAiIsServiceUnavailable() throws Exception {
        postJson("/api/voice/tts", "{\"text\":\"hello\",\"language\":\"en\"}")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_NOT_CONFIGURED"));
    }

    @Test
    void emptyUploadIsRejected() throws Exception {
        mockMvc.perform(
                        multipart("/api/documents/analyze")
                                .file(
                                        new MockMultipartFile(
                                                "file",
                                                "empty.pdf",
                                                "application/pdf",
                                                new byte[0])))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void unknownApiPathAndWrongMethod() throws Exception {
        mockMvc.perform(get("/api/nope")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/voice/query")).andExpect(status().isMethodNotAllowed());
        postJson("/api/chat", "{not json").andExpect(status().isBadRequest());
    }
}
