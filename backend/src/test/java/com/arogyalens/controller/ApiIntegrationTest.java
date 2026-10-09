package com.arogyalens.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

/** End-to-end API tests through the full Spring context with AI disabled (no network). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void analyzeDocumentDemo() throws Exception {
        mockMvc.perform(multipart("/api/documents/analyze").param("demo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentType").value("LAB_REPORT"))
                .andExpect(jsonPath("$.demo").value(true))
                .andExpect(jsonPath("$.parameters").isArray());
    }

    @Test
    void analyzePrescriptionDemo() throws Exception {
        mockMvc.perform(multipart("/api/prescriptions/analyze").param("demo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentType").value("PRESCRIPTION"))
                .andExpect(jsonPath("$.prescriptionItems").isArray());
    }

    @Test
    void analyzeMedicineDemo() throws Exception {
        mockMvc.perform(multipart("/api/medicines/analyze").param("demo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentType").value("MEDICINE"))
                .andExpect(jsonPath("$.medicine.name").value("Metformin"));
    }

    @Test
    void safetyValidate() throws Exception {
        mockMvc.perform(
                        post("/api/safety/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"text\":\"You have diabetes\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.safe").value(false));
    }

    @Test
    void healthReportsConfiguration() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.aiConfigured").value(false))
                .andExpect(jsonPath("$.mapsConfigured").value(false));
    }

    @Test
    void responsesCarrySecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(
                        header().string(
                                        "Content-Security-Policy",
                                        containsString("frame-ancestors 'none'")))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void voiceQueryWorksWithoutSession() throws Exception {
        mockMvc.perform(
                        post("/api/voice/query")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"query\":\"What is paracetamol used for?\",\"language\":\"en\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").isNotEmpty())
                .andExpect(jsonPath("$.fromDocument").value(false));
    }

    @Test
    void chatWorksWithoutSession() throws Exception {
        mockMvc.perform(
                        post("/api/chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"message\":\"What is HbA1c?\",\"language\":\"kn\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").isNotEmpty());
    }

    @Test
    void invalidInputGivesConsistentErrorWithoutInternals() throws Exception {
        mockMvc.perform(
                        post("/api/voice/query")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"query\":\"\",\"language\":\"en\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(content().string(not(containsString("Exception"))));

        mockMvc.perform(
                        post("/api/voice/query")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(
                        post("/api/voice/query")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"query\":\"hi\",\"language\":\"xx\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_LANGUAGE"));
    }

    @Test
    void spoofedUploadIsRejectedByContent() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "x.png", "image/png", "<script>".getBytes());
        mockMvc.perform(multipart("/api/medicines/analyze").file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_FORMAT"));
    }

    @Test
    void imageScanWithoutAiReportsNotConfigured() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        mockMvc.perform(
                        multipart("/api/medicines/analyze")
                                .file(new MockMultipartFile("file", "m.png", "image/png", png)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_NOT_CONFIGURED"))
                .andExpect(jsonPath("$.userMessage").isNotEmpty());
    }

    @Test
    void doctorSpecialtyAndSearch() throws Exception {
        mockMvc.perform(
                        post("/api/doctors/specialty")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"condition\":\"high blood sugar\",\"language\":\"en\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialty").value(containsString("Endocrinologist")));

        mockMvc.perform(
                        post("/api/doctors/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"specialty\":\"Cardiologist\",\"location\":\"Mysuru\",\"language\":\"en\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placesEnabled").value(false))
                .andExpect(jsonPath("$.places").isEmpty())
                .andExpect(jsonPath("$.links[0].label").value("Google Maps"));

        mockMvc.perform(
                        post("/api/doctors/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"specialty\":\"<script>\",\"location\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownSourceIs404() throws Exception {
        mockMvc.perform(get("/api/sources/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SOURCE_NOT_FOUND"));
    }

    @Test
    void spaDeepLinksServeIndexButUnknownApiIs404() throws Exception {
        mockMvc.perform(get("/results"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ArogyaLens test shell")));
        mockMvc.perform(get("/api/does-not-exist")).andExpect(status().isNotFound());
    }

    @Test
    void corsAllowsConfiguredOriginOnly() throws Exception {
        mockMvc.perform(
                        options("/api/health")
                                .header("Origin", "http://localhost:5173")
                                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mockMvc.perform(
                        options("/api/health")
                                .header("Origin", "https://evil.example")
                                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
