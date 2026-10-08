package com.arogyalens.service;

import com.arogyalens.ai.GeminiService;
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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class DischargeSummaryService {

    private final FileValidationUtil fileValidationUtil;
    private final GeminiService geminiService;
    private final SafetyValidationService safetyValidationService;
    private final SourceService sourceService;
    private final SessionService sessionService;
    private final DemoDataService demoDataService;
    private final ArogyaLensProperties properties;
    private final ObjectMapper objectMapper;

    public DischargeSummaryService(FileValidationUtil fileValidationUtil,
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
                throw new ArogyaLensException("DEMO_DISABLED", "Demo disabled",
                        "Demo mode is disabled. Please upload a discharge summary document.");
            }
            String id = sessionService.createId();
            AnalysisResponse response = demoDataService.discharge(id);
            sessionService.save(id, response, "demo discharge");
            return response;
        }
        if (file == null || file.isEmpty()) {
            throw new ArogyaLensException("EMPTY_FILE", "Empty upload",
                    "Please upload a discharge summary image or PDF.");
        }
        fileValidationUtil.validate(file);
        if (!geminiService.isAvailable()) {
            throw new ArogyaLensException("AI_NOT_CONFIGURED", "Gemini API key missing",
                    "Discharge summary analysis needs GEMINI_API_KEY in the project .env file. Restart the backend after adding it.");
        }

        String id = sessionService.createId();
        try {
            Optional<String> ai = geminiService.generateMultimodal(
                    PromptLibrary.dischargePrompt(),
                    file.getBytes(),
                    mime(file)
            );
            if (ai.isEmpty()) {
                throw new ArogyaLensException("AI_EMPTY", "Could not read discharge summary",
                        "We couldn't confidently read this discharge document. Try a clearer scan.");
            }
            JsonNode root = objectMapper.readTree(ai.get());
            DischargeSummary summary = new DischargeSummary(
                    safe(root.path("reasonForAdmission").asText("Unable to confidently determine from the uploaded document.")),
                    safe(root.path("treatmentPerformed").asText("Unable to confidently determine from the uploaded document.")),
                    readList(root.path("importantFindings")),
                    readList(root.path("medicinesListed")),
                    readList(root.path("followUpInstructions")),
                    readList(root.path("warningSigns")),
                    readList(root.path("doctorQuestions"))
            );
            AnalysisResponse base = demoDataService.discharge(id);
            AnalysisResponse response = new AnalysisResponse(
                    id, base.documentType(), base.documentLabel(), false, base.privacyShield(),
                    base.processingSteps(), 2, 0, List.of(), base.dashboard(), summary.doctorQuestions(),
                    sourceService.forTopic("discharge"), base.safetyNotes(), null, null, summary,
                    base.familySummary(), java.util.Map.of(),
                    "Original discharge document retained for verification. Files are processed temporarily.",
                    true, base.disclaimer()
            );
            sessionService.save(id, response, ai.get());
            return response;
        } catch (ArogyaLensException ex) {
            throw ex;
        } catch (Exception e) {
            throw new ArogyaLensException("ANALYSIS_FAILED", "Discharge analysis failed",
                    "We couldn't analyze this discharge document. Please try a clearer file.");
        }
    }

    private String mime(MultipartFile file) {
        if (file.getContentType() != null && !file.getContentType().isBlank()) return file.getContentType();
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".pdf")) return "application/pdf";
        return "image/jpeg";
    }

    private String safe(String text) {
        return safetyValidationService.enforceSafeWording(text);
    }

    private List<String> readList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(n -> list.add(safe(n.asText())));
        }
        return list;
    }
}
