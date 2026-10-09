package com.arogyalens.demo;

import com.arogyalens.dto.AnalysisResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Deterministic sample analyses used by demo mode and as the base for AI responses. The samples
 * live in {@code src/main/resources/demo/*.json} (synthetic data, not real patients) and are loaded
 * once at startup; a missing or malformed file fails fast with a clear message.
 */
@Service
public class DemoDataService {

    /** Classpath locations of the bundled samples. */
    static final String LAB_REPORT = "/demo/lab-report.json";

    static final String MEDICINE = "/demo/medicine.json";
    static final String PRESCRIPTION = "/demo/prescription.json";
    static final String DISCHARGE = "/demo/discharge.json";

    private final AnalysisResponse labReport;
    private final AnalysisResponse medicine;
    private final AnalysisResponse prescription;
    private final AnalysisResponse discharge;

    public DemoDataService(ObjectMapper objectMapper) {
        this.labReport = load(objectMapper, LAB_REPORT);
        this.medicine = load(objectMapper, MEDICINE);
        this.prescription = load(objectMapper, PRESCRIPTION);
        this.discharge = load(objectMapper, DISCHARGE);
    }

    /** Sample blood test report with values in and out of range. */
    public AnalysisResponse labReport(String sessionId) {
        return withSession(labReport, sessionId);
    }

    /** Sample medicine package (paracetamol). */
    public AnalysisResponse medicine(String sessionId) {
        return withSession(medicine, sessionId);
    }

    /** Sample prescription with a morning/afternoon/night schedule. */
    public AnalysisResponse prescription(String sessionId) {
        return withSession(prescription, sessionId);
    }

    /** Sample hospital discharge summary. */
    public AnalysisResponse discharge(String sessionId) {
        return withSession(discharge, sessionId);
    }

    private static AnalysisResponse load(ObjectMapper objectMapper, String path) {
        try (InputStream in = DemoDataService.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Demo data missing on classpath: " + path);
            }
            return objectMapper.readValue(in, AnalysisResponse.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read demo data " + path, e);
        }
    }

    private static AnalysisResponse withSession(AnalysisResponse base, String sessionId) {
        String id = sessionId == null ? UUID.randomUUID().toString() : sessionId;
        return new AnalysisResponse(
                id,
                base.documentType(),
                base.documentLabel(),
                base.demo(),
                base.privacyShield(),
                base.processingSteps(),
                base.pages(),
                base.parametersDetected(),
                base.parameters(),
                base.dashboard(),
                base.doctorQuestions(),
                base.sources(),
                base.safetyNotes(),
                base.medicine(),
                base.prescriptionItems(),
                base.dischargeSummary(),
                base.familySummary(),
                base.translations(),
                base.originalPreviewNote(),
                base.aiUsed(),
                base.disclaimer());
    }
}
