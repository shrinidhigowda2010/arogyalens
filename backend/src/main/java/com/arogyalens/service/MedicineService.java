package com.arogyalens.service;

import com.arogyalens.ai.AiErrors;
import com.arogyalens.ai.GeminiService;
import com.arogyalens.ai.PromptLibrary;
import com.arogyalens.config.ArogyaLensProperties;
import com.arogyalens.demo.DemoDataService;
import com.arogyalens.dto.AnalysisResponse;
import com.arogyalens.exception.ArogyaLensException;
import com.arogyalens.model.MedicineInfo;
import com.arogyalens.safety.SafetyValidationService;
import com.arogyalens.source.SourceService;
import com.arogyalens.util.FileValidationUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MedicineService {

    private final FileValidationUtil fileValidationUtil;
    private final GeminiService geminiService;
    private final SafetyValidationService safetyValidationService;
    private final SourceService sourceService;
    private final SessionService sessionService;
    private final DemoDataService demoDataService;
    private final ArogyaLensProperties properties;
    private final ObjectMapper objectMapper;

    public MedicineService(
            FileValidationUtil fileValidationUtil,
            GeminiService geminiService,
            SafetyValidationService safetyValidationService,
            SourceService sourceService,
            SessionService sessionService,
            DemoDataService demoDataService,
            ArogyaLensProperties properties,
            ObjectMapper objectMapper) {
        this.fileValidationUtil = fileValidationUtil;
        this.geminiService = geminiService;
        this.safetyValidationService = safetyValidationService;
        this.sourceService = sourceService;
        this.sessionService = sessionService;
        this.demoDataService = demoDataService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public AnalysisResponse analyze(MultipartFile file, boolean demo) {
        if (demo) {
            if (!properties.demo().enabled()) {
                throw new ArogyaLensException(
                        "DEMO_DISABLED",
                        "Demo disabled",
                        "Demo mode is disabled. Please upload a medicine package image.");
            }
            String id = sessionService.createId();
            AnalysisResponse response = demoDataService.medicine(id);
            sessionService.save(id, response, "demo medicine");
            return response;
        }
        if (file == null || file.isEmpty()) {
            throw new ArogyaLensException(
                    "EMPTY_FILE",
                    "Empty upload",
                    "Please upload a photo of the medicine strip or package.");
        }
        String mimeType = fileValidationUtil.validate(file);
        if (!geminiService.isAvailable()) {
            throw AiErrors.notConfigured();
        }

        String id = sessionService.createId();
        try {
            String ai =
                    geminiService.generateJson(
                            PromptLibrary.medicinePrompt(),
                            file.getBytes(),
                            mimeType,
                            "medicine label");
            JsonNode root = objectMapper.readTree(ai);
            MedicineInfo medicine =
                    new MedicineInfo(
                            root.path("name").asText("Unable to confidently identify"),
                            root.path("strength").asText(null),
                            root.path("dosageForm").asText(null),
                            root.path("manufacturer").asText(null),
                            safetyValidationService.enforceSafeWording(
                                    root.path("generalUse")
                                            .asText("General information unavailable.")),
                            readList(root.path("commonSideEffects")),
                            readList(root.path("precautions")),
                            readListOrDefault(
                                    root.path("warnings"),
                                    List.of(
                                            "Follow the prescription provided by your healthcare professional.")),
                            root.path("confidence").asDouble(0.5),
                            sourceService.forTopic(root.path("name").asText("medicine")));

            AnalysisResponse base = demoDataService.medicine(id);
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
                            medicine.sources(),
                            base.safetyNotes(),
                            medicine,
                            null,
                            null,
                            base.familySummary(),
                            java.util.Map.of(),
                            "Original medicine image retained for verification. Files are processed temporarily.",
                            true,
                            base.disclaimer());
            sessionService.save(id, response, ai);
            return response;
        } catch (ArogyaLensException ex) {
            throw ex;
        } catch (Exception e) {
            throw new ArogyaLensException(
                    "ANALYSIS_FAILED",
                    "Medicine analysis failed",
                    "We couldn't analyze this medicine image. Please try a clearer photo.");
        }
    }

    private List<String> readList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(n -> list.add(safetyValidationService.enforceSafeWording(n.asText())));
        }
        return list;
    }

    private List<String> readListOrDefault(JsonNode node, List<String> fallback) {
        List<String> list = readList(node);
        return list.isEmpty() ? fallback : list;
    }
}
