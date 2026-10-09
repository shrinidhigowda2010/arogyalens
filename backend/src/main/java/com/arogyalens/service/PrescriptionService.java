package com.arogyalens.service;

import com.arogyalens.ai.AiErrors;
import com.arogyalens.ai.GeminiService;
import com.arogyalens.ai.PromptLibrary;
import com.arogyalens.config.ArogyaLensProperties;
import com.arogyalens.demo.DemoDataService;
import com.arogyalens.dto.AnalysisResponse;
import com.arogyalens.exception.ArogyaLensException;
import com.arogyalens.model.PrescriptionItem;
import com.arogyalens.privacy.PrivacyService;
import com.arogyalens.safety.SafetyValidationService;
import com.arogyalens.source.SourceService;
import com.arogyalens.util.FileValidationUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Turns prescription photos into a readable daily schedule. */
@Service
public class PrescriptionService {

    private final FileValidationUtil fileValidationUtil;
    private final PrivacyService privacyService;
    private final GeminiService geminiService;
    private final SafetyValidationService safetyValidationService;
    private final SourceService sourceService;
    private final SessionService sessionService;
    private final DemoDataService demoDataService;
    private final ScanSupport scanSupport;
    private final LocalDocumentParser localDocumentParser;
    private final ArogyaLensProperties properties;
    private final ObjectMapper objectMapper;

    public PrescriptionService(
            FileValidationUtil fileValidationUtil,
            PrivacyService privacyService,
            GeminiService geminiService,
            SafetyValidationService safetyValidationService,
            SourceService sourceService,
            SessionService sessionService,
            DemoDataService demoDataService,
            ScanSupport scanSupport,
            LocalDocumentParser localDocumentParser,
            ArogyaLensProperties properties,
            ObjectMapper objectMapper) {
        this.fileValidationUtil = fileValidationUtil;
        this.privacyService = privacyService;
        this.geminiService = geminiService;
        this.safetyValidationService = safetyValidationService;
        this.sourceService = sourceService;
        this.sessionService = sessionService;
        this.demoDataService = demoDataService;
        this.scanSupport = scanSupport;
        this.localDocumentParser = localDocumentParser;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public AnalysisResponse analyze(MultipartFile file, boolean demo) {
        Optional<AnalysisResponse> sample =
                scanSupport.demoIfRequested(
                        demo,
                        demoDataService::prescription,
                        "demo prescription",
                        "Please upload a prescription image or PDF.");
        if (sample.isPresent()) {
            return sample.get();
        }
        scanSupport.requireFile(file, "Please upload a prescription image or PDF.");
        String mimeType = fileValidationUtil.validate(file);
        String id = sessionService.createId();

        try {
            Optional<String> text = localDocumentParser.extractText(file);
            if (text.isPresent()) {
                List<PrescriptionItem> localItems =
                        localDocumentParser.parsePrescription(text.get());
                if (!localItems.isEmpty()) {
                    String redacted = privacyService.scanAndRedact(text.get()).redactedText();
                    return wrapItems(id, localItems, false, redacted);
                }
            }

            if (!geminiService.isAvailable()) {
                throw AiErrors.notConfigured();
            }

            String ai =
                    geminiService.generateJson(
                            PromptLibrary.prescriptionPrompt(),
                            file.getBytes(),
                            mimeType,
                            "prescription");

            JsonNode root = objectMapper.readTree(ai);
            List<PrescriptionItem> items = new ArrayList<>();
            for (JsonNode node : root.path("items")) {
                boolean confident = node.path("confident").asBoolean(false);
                items.add(
                        new PrescriptionItem(
                                node.path("medicineName").asText(null),
                                node.path("strength").asText(null),
                                node.path("frequency").asText(null),
                                node.path("timing").asText(null),
                                node.path("foodRelation").asText(null),
                                node.path("morning").asBoolean(false),
                                node.path("afternoon").asBoolean(false),
                                node.path("night").asBoolean(false),
                                confident,
                                confident
                                        ? null
                                        : "⚠️ We could not confidently read this instruction. Please confirm it with your doctor or pharmacist."));
            }
            if (items.isEmpty()) {
                throw new ArogyaLensException(
                        "AI_EMPTY",
                        "No prescription lines",
                        "We couldn't confidently extract medicine instructions from this file.");
            }
            return wrapItems(id, items, true, ai);
        } catch (ArogyaLensException ex) {
            throw ex;
        } catch (IOException e) {
            throw ScanSupport.analysisFailed(
                    "Prescription",
                    "We couldn't analyze this prescription. Please try a clearer image or PDF.");
        }
    }

    private AnalysisResponse wrapItems(
            String id, List<PrescriptionItem> items, boolean aiUsed, String context) {
        AnalysisResponse base = demoDataService.prescription(id);
        AnalysisResponse response =
                new AnalysisResponse(
                        id,
                        base.documentType(),
                        base.documentLabel(),
                        false,
                        base.privacyShield(),
                        base.processingSteps(),
                        1,
                        0,
                        List.of(),
                        base.dashboard(),
                        base.doctorQuestions(),
                        sourceService.forTopic("medicine"),
                        base.safetyNotes(),
                        null,
                        items,
                        null,
                        base.familySummary(),
                        java.util.Map.of(),
                        "Original prescription retained for verification. Files are processed temporarily.",
                        aiUsed,
                        base.disclaimer());
        sessionService.save(id, response, context);
        return response;
    }
}
