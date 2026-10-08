package com.arogyalens.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
        mockMvc.perform(post("/api/safety/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"You have diabetes\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.safe").value(false));
    }
}
