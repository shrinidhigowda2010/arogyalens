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
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Identifies medicines from package photos and returns general information. */
@Service
public class MedicineService {

    private final FileValidationUtil fileValidationUtil;
    private final GeminiService geminiService;
    private final SafetyValidationService safetyValidationService;
    private final SourceService sourceService;
    private final SessionService sessionService;
    private final DemoDataService demoDataService;
    private final ScanSupport scanSupport;
    private final ArogyaLensProperties properties;
    private final ObjectMapper objectMapper;

    public MedicineService(
            FileValidationUtil fileValidationUtil,
            GeminiService geminiService,
            SafetyValidationService safetyValidationService,
            SourceService sourceService,
            SessionService sessionService,
            DemoDataService demoDataService,
            ScanSupport scanSupport,
            ArogyaLensProperties properties,
            ObjectMapper objectMapper) {
        this.fileValidationUtil = fileValidationUtil;
        this.geminiService = geminiService;
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
                        demoDataService::medicine,
                        "demo medicine",
                        "Please upload a medicine package image.");
        if (sample.isPresent()) {
            return sample.get();
        }
        scanSupport.requireFile(file, "Please upload a photo of the medicine strip or package.");
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
            MedicineInfo medicine = toMedicine(root);

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
        } catch (IOException e) {
            throw ScanSupport.analysisFailed(
                    "Medicine",
                    "We couldn't analyze this medicine image. Please try a clearer photo.");
        }
    }

    /** Maps the model JSON to {@link MedicineInfo}, applying safe wording and defaults. */
    private MedicineInfo toMedicine(JsonNode root) {
        return new MedicineInfo(
                root.path("name").asText("Unable to confidently identify"),
                root.path("strength").asText(null),
                root.path("dosageForm").asText(null),
                root.path("manufacturer").asText(null),
                safetyValidationService.enforceSafeWording(
                        root.path("generalUse").asText("General information unavailable.")),
                scanSupport.safeList(root.path("commonSideEffects")),
                scanSupport.safeList(root.path("precautions")),
                readListOrDefault(
                        root.path("warnings"),
                        List.of(
                                "Follow the prescription provided by your healthcare professional.")),
                root.path("confidence").asDouble(0.5),
                sourceService.forTopic(root.path("name").asText("medicine")));
    }

    private List<String> readListOrDefault(JsonNode node, List<String> fallback) {
        List<String> list = scanSupport.safeList(node);
        return list.isEmpty() ? fallback : list;
    }
}
