package com.arogyalens.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arogyalens.ai.AiErrors;
import com.arogyalens.ai.GeminiService;
import java.io.InputStream;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Main user flows end to end (upload → privacy → AI → safety → response) with Gemini mocked at the
 * service boundary, so no network calls are made.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScanFlowIntegrationTest {

    private static final byte[] PNG = {
        (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'
    };

    @Autowired MockMvc mockMvc;

    @MockBean GeminiService gemini;

    @BeforeEach
    void aiOn() {
        when(gemini.isAvailable()).thenReturn(true);
    }

    private static MockMultipartFile png(String name) {
        return new MockMultipartFile("file", name, "image/png", PNG);
    }

    private static MockMultipartFile samplePdf() throws Exception {
        try (InputStream in =
                ScanFlowIntegrationTest.class.getResourceAsStream(
                        "/fixtures/sample-lab-report.pdf")) {
            return new MockMultipartFile(
                    "file", "report.pdf", "application/pdf", in.readAllBytes());
        }
    }

    @Test
    void labReportPhotoIsExplainedWithSafetyAndPrivacy() throws Exception {
        when(gemini.generateJson(anyString(), any(byte[].class), eq("image/png"), eq("document")))
                .thenReturn(
                        """
                        {"documentType":"LAB_REPORT","documentLabel":"Blood Test",
                         "rawTextSummary":"Patient Name: Ravi Kumar, phone 9876543210",
                         "parameters":[
                          {"name":"HbA1c","value":"7.8","unit":"%","referenceRange":"<5.7",
                           "status":"IMPORTANT_ATTENTION","confidence":0.95,
                           "explanation":"You have diabetes.","simpleExplanation":"Sugar marker"},
                          {"name":"Hemoglobin","value":"","status":"bogus","confidence":0.4}]}
                        """);

        mockMvc.perform(
                        multipart("/api/documents/analyze")
                                .file(png("r.png"))
                                .param("language", "hi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiUsed").value(true))
                .andExpect(jsonPath("$.parametersDetected").value(2))
                .andExpect(jsonPath("$.parameters[1].status").value("LOW_CONFIDENCE"))
                .andExpect(jsonPath("$.dashboard.importantAttention").value(1))
                .andExpect(jsonPath("$.privacyShield.redacted").value(true))
                .andExpect(content().string(not(containsString("You have diabetes"))))
                .andExpect(content().string(not(containsString("9876543210"))));
    }

    @Test
    void textPdfWorksOfflineWhenAiFails() throws Exception {
        when(gemini.generateJson(anyString(), any(byte[].class), anyString(), anyString()))
                .thenThrow(AiErrors.busy());

        mockMvc.perform(multipart("/api/documents/analyze").file(samplePdf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiUsed").value(false))
                .andExpect(jsonPath("$.parametersDetected").value(greaterThan(3)))
                .andExpect(content().string(not(containsString("UHID-999"))));
    }

    @Test
    void aiFailureOnPhotoReturnsTypedError() throws Exception {
        when(gemini.generateJson(anyString(), any(byte[].class), anyString(), anyString()))
                .thenThrow(AiErrors.quota());

        mockMvc.perform(multipart("/api/documents/analyze").file(png("r.png")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_QUOTA"));
    }

    @Test
    void medicinePhoto() throws Exception {
        when(gemini.generateJson(anyString(), any(byte[].class), anyString(), eq("medicine label")))
                .thenReturn(
                        """
                        {"name":"Paracetamol","strength":"500 mg","dosageForm":"Tablet",
                         "manufacturer":"Acme","generalUse":"Fever and pain",
                         "commonSideEffects":["Nausea"],"precautions":["Liver disease"],
                         "warnings":["Stop taking other medicines"],"confidence":0.9}
                        """);

        mockMvc.perform(multipart("/api/medicines/analyze").file(png("m.png")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicine.name").value("Paracetamol"));
    }

    @Test
    void prescriptionPhoto() throws Exception {
        when(gemini.generateJson(anyString(), any(byte[].class), anyString(), eq("prescription")))
                .thenReturn(
                        """
                        {"items":[{"medicineName":"Metformin","strength":"500 mg",
                          "frequency":"Twice daily","timing":"After food","foodRelation":"After food",
                          "morning":true,"afternoon":false,"night":true,"confident":true},
                         {"medicineName":"Unclear","confident":false}]}
                        """);

        mockMvc.perform(multipart("/api/prescriptions/analyze").file(png("p.png")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prescriptionItems[0].medicineName").value("Metformin"))
                .andExpect(jsonPath("$.prescriptionItems.length()").value(2));
    }

    @Test
    void dischargePhoto() throws Exception {
        when(gemini.generateJson(
                        anyString(), any(byte[].class), anyString(), eq("discharge summary")))
                .thenReturn(
                        """
                        {"reasonForAdmission":"Fever","treatmentPerformed":"IV fluids",
                         "importantFindings":["Dengue NS1 positive"],"medicinesListed":["Paracetamol"],
                         "followUpInstructions":["Review in 3 days"],"warningSigns":["Bleeding"],
                         "doctorQuestions":["When can I resume work?"]}
                        """);

        mockMvc.perform(multipart("/api/discharge/analyze").file(png("d.png")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dischargeSummary.reasonForAdmission").value("Fever"));
    }

    @Test
    void askAndTranslateUseAiText() throws Exception {
        when(gemini.generateText(anyString()))
                .thenReturn(
                        Optional.of(
                                "{\"answer\":\"Fever is a raised temperature.\",\"translated\":\"बुखार\",\"questions\":[\"Q1?\"]}"));

        mockMvc.perform(
                        post("/api/voice/query")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"query\":\"what is fever\",\"language\":\"hi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").isNotEmpty());
        mockMvc.perform(
                        post("/api/translate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"text\":\"Fever\",\"targetLanguage\":\"hi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translated").isNotEmpty());
    }
}
