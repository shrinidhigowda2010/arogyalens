package com.arogyalens.service;

import com.arogyalens.ai.AiClient;
import com.arogyalens.ai.AiErrors;
import com.arogyalens.ai.PromptLibrary;
import com.arogyalens.config.ArogyaLensProperties;
import com.arogyalens.demo.DemoDataService;
import com.arogyalens.dto.AnalysisResponse;
import com.arogyalens.exception.ArogyaLensException;
import com.arogyalens.model.DischargeSummary;
import com.arogyalens.safety.SafetyValidationService;
import com.arogyalens.source.SourceService;
import com.arogyalens.util.FileValidationUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Explains discharge summaries using Gemini, with a demo fallback. */
@Service
public class DischargeSummaryService {

    private final FileValidationUtil fileValidationUtil;
    private final AiClient aiClient;
    private final SafetyValidationService safetyValidationService;
    private final SourceService sourceService;
    private final SessionService sessionService;
    private final DemoDataService demoDataService;
    private final ScanSupport scanSupport;
    private final ArogyaLensProperties properties;
    private final ObjectMapper objectMapper;

    public DischargeSummaryService(
            FileValidationUtil fileValidationUtil,
            AiClient aiClient,
            SafetyValidationService safetyValidationService,
            SourceService sourceService,
            SessionService sessionService,
            DemoDataService demoDataService,
            ScanSupport scanSupport,
            ArogyaLensProperties properties,
            ObjectMapper objectMapper) {
        this.fileValidationUtil = fileValidationUtil;
        this.aiClient = aiClient;
        this.safetyValidationService = safetyValidationService;
        this.sourceService = sourceService;
        this.sessionService = sessionService;
        this.demoDataService = demoDataService;
        this.scanSupport = scanSupport;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public AnalysisResponse analyze(MultipartFile file, boolean demo) {
        Optional<AnalysisResponse> sample =
                scanSupport.demoIfRequested(
                        demo,
                        demoDataService::discharge,
                        "demo discharge",
                        "Please upload a discharge summary document.");
        if (sample.isPresent()) {
            return sample.get();
        }
        scanSupport.requireFile(file, "Please upload a discharge summary image or PDF.");
        String mimeType = fileValidationUtil.validate(file);
        if (!aiClient.isAvailable()) {
            throw AiErrors.notConfigured();
        }

        String id = sessionService.createId();
        try {
            String ai =
                    aiClient.generateJson(
                            PromptLibrary.dischargePrompt(),
                            file.getBytes(),
                            mimeType,
                            "discharge summary");
            JsonNode root = objectMapper.readTree(ai);
            DischargeSummary summary = toSummary(root);
            AnalysisResponse base = demoDataService.discharge(id);
            AnalysisResponse response =
                    new AnalysisResponse(
                            id,
                            base.documentType(),
                            base.documentLabel(),
                            false,
                            base.privacyShield(),
                            base.processingSteps(),
                            2,
                            0,
                            List.of(),
                            base.dashboard(),
                            summary.doctorQuestions(),
                            sourceService.forTopic("discharge"),
                            base.safetyNotes(),
                            null,
                            null,
                            summary,
                            base.familySummary(),
                            java.util.Map.of(),
                            "Original discharge document retained for verification. Files are processed temporarily.",
                            true,
                            base.disclaimer());
            sessionService.save(id, response, ai);
            return response;
        } catch (ArogyaLensException ex) {
            throw ex;
        } catch (IOException e) {
            throw ScanSupport.analysisFailed(
                    "Discharge",
                    "We couldn't analyze this discharge document. Please try a clearer file.");
        }
    }

    /** Maps the model JSON to a {@link DischargeSummary} with safe wording and defaults. */
    private DischargeSummary toSummary(JsonNode root) {
        return new DischargeSummary(
                safe(
                        root.path("reasonForAdmission")
                                .asText(
                                        "Unable to confidently determine from the uploaded document.")),
                safe(
                        root.path("treatmentPerformed")
                                .asText(
                                        "Unable to confidently determine from the uploaded document.")),
                scanSupport.safeList(root.path("importantFindings")),
                scanSupport.safeList(root.path("medicinesListed")),
                scanSupport.safeList(root.path("followUpInstructions")),
                scanSupport.safeList(root.path("warningSigns")),
                scanSupport.safeList(root.path("doctorQuestions")));
    }

    private String safe(String text) {
        return safetyValidationService.enforceSafeWording(text);
    }
}
