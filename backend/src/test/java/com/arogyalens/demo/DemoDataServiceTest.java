package com.arogyalens.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.arogyalens.dto.AnalysisResponse;
import com.arogyalens.model.DocumentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class DemoDataServiceTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final DemoDataService demo = new DemoDataService(mapper);

    @Test
    void everySampleLoadsWithItsDocumentType() {
        assertThat(demo.labReport("s").documentType()).isEqualTo(DocumentType.LAB_REPORT);
        assertThat(demo.labReport("s").parameters()).isNotEmpty();
        assertThat(demo.medicine("s").medicine().name()).isNotBlank();
        assertThat(demo.prescription("s").prescriptionItems()).isNotEmpty();
        assertThat(demo.discharge("s").dischargeSummary().reasonForAdmission()).isNotBlank();
    }

    @Test
    void sessionIdIsAppliedOrGenerated() {
        assertThat(demo.medicine("abc").sessionId()).isEqualTo("abc");
        assertThat(demo.medicine(null).sessionId()).hasSize(36);
    }

    @Test
    void noFieldIsLostWhenLoadingTheJsonFiles() throws Exception {
        Map<String, Function<String, AnalysisResponse>> samples =
                Map.of(
                        DemoDataService.LAB_REPORT, demo::labReport,
                        DemoDataService.MEDICINE, demo::medicine,
                        DemoDataService.PRESCRIPTION, demo::prescription,
                        DemoDataService.DISCHARGE, demo::discharge);
        for (var entry : samples.entrySet()) {
            try (InputStream in = getClass().getResourceAsStream(entry.getKey())) {
                ObjectNode file = (ObjectNode) mapper.readTree(in);
                ObjectNode loaded = mapper.valueToTree(entry.getValue().apply("x"));
                loaded.remove("sessionId");
                assertThat(loaded).as(entry.getKey()).isEqualTo(file);
            }
        }
    }
}
