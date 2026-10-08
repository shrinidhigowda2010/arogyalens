package com.arogyalens.service;

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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class PrescriptionService {

    private final FileValidationUtil fileValidationUtil;
    private final PrivacyService privacyService;
    private final GeminiService geminiService;
    private final SafetyValidationService safetyValidationService;
    private final SourceService sourceService;
    private final SessionService sessionService;
    private final DemoDataService demoDataService;
    private final LocalDocumentParser localDocumentParser;
    private final ArogyaLensProperties properties;
    private final ObjectMapper objectMapper;

    public PrescriptionService(FileValidationUtil fileValidationUtil,
                               PrivacyService privacyService,
                               GeminiService geminiService,
                               SafetyValidationService safetyValidationService,
                               SourceService sourceService,
                               SessionService sessionService,
                               DemoDataService demoDataService,
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
        this.localDocumentParser = localDocumentParser;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public AnalysisResponse analyze(MultipartFile file, boolean demo) {
        if (demo) {
            if (!properties.demo().enabled()) {
                throw new ArogyaLensException("DEMO_DISABLED", "Demo disabled",
                        "Demo mode is disabled. Please upload a prescription image or PDF.");
            }
            String id = sessionService.createId();
            AnalysisResponse response = demoDataService.prescription(id);
            sessionService.save(id, response, "demo prescription");
            return response;
        }
        if (file == null || file.isEmpty()) {
            throw new ArogyaLensException("EMPTY_FILE", "Empty upload",
                    "Please upload a prescription image or PDF.");
        }
        fileValidationUtil.validate(file);
        String id = sessionService.createId();

        try {
            Optional<String> text = localDocumentParser.extractText(file);
            if (text.isPresent()) {
                List<PrescriptionItem> localItems = localDocumentParser.parsePrescription(text.get());
                if (!localItems.isEmpty()) {
                    privacyService.scanAndRedact(text.get());
                    return wrapItems(id, localItems, false, text.get());
                }
            }

            if (!geminiService.isAvailable()) {
                throw new ArogyaLensException("AI_NOT_CONFIGURED", "Gemini API key missing",
                        "Prescription image recognition needs GEMINI_API_KEY in .env. Text-based prescription PDFs can still be parsed without AI.");
            }

            Optional<String> ai = geminiService.generateMultimodal(
                    PromptLibrary.prescriptionPrompt(),
                    file.getBytes(),
                    mime(file)
            );
            if (ai.isEmpty()) {
                throw new ArogyaLensException("AI_EMPTY", "Could not read prescription",
                        "We couldn't confidently read this prescription. Try a clearer photo.");
            }

            JsonNode root = objectMapper.readTree(ai.get());
            List<PrescriptionItem> items = new ArrayList<>();
            for (JsonNode node : root.path("items")) {
                boolean confident = node.path("confident").asBoolean(false);
                items.add(new PrescriptionItem(
                        node.path("medicineName").asText(null),
                        node.path("strength").asText(null),
                        node.path("frequency").asText(null),
                        node.path("timing").asText(null),
                        node.path("foodRelation").asText(null),
                        node.path("morning").asBoolean(false),
                        node.path("afternoon").asBoolean(false),
                        node.path("night").asBoolean(false),
                        confident,
                        confident ? null
                                : "⚠️ We could not confidently read this instruction. Please confirm it with your doctor or pharmacist."
                ));
            }
            if (items.isEmpty()) {
                throw new ArogyaLensException("AI_EMPTY", "No prescription lines",
                        "We couldn't confidently extract medicine instructions from this file.");
            }
            return wrapItems(id, items, true, ai.get());
        } catch (ArogyaLensException ex) {
            throw ex;
        } catch (Exception e) {
            throw new ArogyaLensException("ANALYSIS_FAILED", "Prescription analysis failed",
                    "We couldn't analyze this prescription. Please try a clearer image or PDF.");
        }
    }

    private AnalysisResponse wrapItems(String id, List<PrescriptionItem> items, boolean aiUsed, String context) {
        AnalysisResponse base = demoDataService.prescription(id);
        AnalysisResponse response = new AnalysisResponse(
                id, base.documentType(), base.documentLabel(), false, base.privacyShield(),
                base.processingSteps(), 1, 0, List.of(), base.dashboard(), base.doctorQuestions(),
                sourceService.forTopic("medicine"), base.safetyNotes(), null, items, null,
                base.familySummary(), java.util.Map.of(),
                "Original prescription retained for verification. Files are processed temporarily.",
                aiUsed, base.disclaimer()
        );
        sessionService.save(id, response, context);
        return response;
    }

    private String mime(MultipartFile file) {
        if (file.getContentType() != null && !file.getContentType().isBlank()) return file.getContentType();
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".pdf")) return "application/pdf";
        return "image/jpeg";
    }
}
