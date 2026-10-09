package com.arogyalens.history;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end history flow on the H2 fallback database with Flyway migrations. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HistoryApiTest {

    @Autowired
    MockMvc mockMvc;

    private static String device() {
        return UUID.randomUUID().toString();
    }

    @Test
    void nothingIsStoredWithoutOptIn() throws Exception {
        mockMvc.perform(post("/api/voice/query").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"what is fever\",\"language\":\"en\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/history"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DEVICE_ID_REQUIRED"));
    }

    @Test
    void storesMaskedChatAndScanThenDeletes() throws Exception {
        String id = device();
        mockMvc.perform(post("/api/voice/query").header("X-Device-Id", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"I am Ravi, phone 9876543210, what is fever?\",\"language\":\"en\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(multipart("/api/documents/analyze").param("demo", "true").header("X-Device-Id", id))
                .andExpect(status().isOk());

        String body = mockMvc.perform(get("/api/history").header("X-Device-Id", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].kind").value("SCAN"))
                .andExpect(jsonPath("$[1].kind").value("CHAT"))
                .andExpect(content().string(not(containsString("9876543210"))))
                .andReturn().getResponse().getContentAsString();
        String firstId = body.replaceAll("(?s)^\\[\\{\"id\":(\\d+).*", "$1");

        // another device cannot see or delete it
        mockMvc.perform(get("/api/history").header("X-Device-Id", device())).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(delete("/api/history/" + firstId).header("X-Device-Id", device()))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/history/" + firstId).header("X-Device-Id", id)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/history").header("X-Device-Id", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.deleted").value(1));
        mockMvc.perform(get("/api/history").header("X-Device-Id", id)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rejectsMalformedDeviceIds() throws Exception {
        mockMvc.perform(get("/api/history").header("X-Device-Id", "short"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/health")).andExpect(jsonPath("$.historyEnabled").value(true));
    }
}
