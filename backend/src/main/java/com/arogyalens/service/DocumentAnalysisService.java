package com.arogyalens.service;

import com.arogyalens.ai.GeminiService;
import com.arogyalens.ai.PromptLibrary;
import com.arogyalens.config.ArogyaLensProperties;
import com.arogyalens.demo.DemoDataService;
import com.arogyalens.dto.AnalysisResponse;
import com.arogyalens.dto.AnalysisResponse.DashboardSummaryDto;
import com.arogyalens.dto.AnalysisResponse.PrivacyShieldDto;
import com.arogyalens.dto.AnalysisResponse.ProcessingStepDto;
import com.arogyalens.exception.ArogyaLensException;
import com.arogyalens.model.DocumentType;
import com.arogyalens.model.MedicalParameter;
import com.arogyalens.model.ParameterStatus;
import com.arogyalens.privacy.PrivacyService;
import com.arogyalens.safety.SafetyValidationService;
import com.arogyalens.source.SourceService;
import com.arogyalens.util.FileValidationUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class DocumentAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(DocumentAnalysisService.class);

    private final FileValidationUtil fileValidationUtil;
    private final PrivacyService privacyService;
    private final GeminiService geminiService;
    private final SafetyValidationService safetyValidationService;
    private final SourceService sourceService;
    private final SessionService sessionService;
    private final DoctorQuestionService doctorQuestionService;
    private final LocalDocumentParser localDocumentParser;
    private final OfflineLanguagePack offlineLanguagePack;
    private final DemoDataService demoDataService;
    private final ArogyaLensProperties properties;
    private final ObjectMapper objectMapper;

    public DocumentAnalysisService(FileValidationUtil fileValidationUtil,
                                   PrivacyService privacyService,
                                   GeminiService geminiService,
                                   SafetyValidationService safetyValidationService,
                                   SourceService sourceService,
                                   SessionService sessionService,
                                   DoctorQuestionService doctorQuestionService,
                                   LocalDocumentParser localDocumentParser,
                                   OfflineLanguagePack offlineLanguagePack,
                                   DemoDataService demoDataService,
                                   ArogyaLensProperties properties,
                                   ObjectMapper objectMapper) {
        this.fileValidationUtil = fileValidationUtil;
        this.privacyService = privacyService;
        this.geminiService = geminiService;
        this.safetyValidationService = safetyValidationService;
        this.sourceService = sourceService;
        this.sessionService = sessionService;
        this.doctorQuestionService = doctorQuestionService;
        this.localDocumentParser = localDocumentParser;
        this.offlineLanguagePack = offlineLanguagePack;
        this.demoDataService = demoDataService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public AnalysisResponse analyze(MultipartFile file, boolean demo, String hint, String language) {
        if (demo) {
            if (!properties.demo().enabled()) {
                throw new ArogyaLensException(
                        "DEMO_DISABLED",
                        "Demo disabled",
                        "Demo mode is disabled. Please upload a JPG, PNG, or PDF medical document."
                );
            }
            String id = sessionService.createId();
            AnalysisResponse response = demoDataService.labReport(id);
            sessionService.save(id, response, "demo");
            return response;
        }
        if (file == null || file.isEmpty()) {
            throw new ArogyaLensException(
                    "EMPTY_FILE",
                    "Empty upload",
                    "Please choose a medical document image or PDF to analyze."
            );
        }

        fileValidationUtil.validate(file);
        String id = sessionService.createId();
        String lang = language == null || language.isBlank() ? "en" : language;

        try {
            Optional<String> extractedText = localDocumentParser.extractText(file);
            Optional<String> aiJson = Optional.empty();

            if (geminiService.isAvailable()) {
                aiJson = geminiService.generateMultimodal(
                        PromptLibrary.documentExtractionPrompt(hint),
                        file.getBytes(),
                        resolveMime(file)
                );
            }

            if (aiJson.isPresent()) {
                return fromAi(id, aiJson.get(), lang);
            }

            if (extractedText.isPresent()) {
                List<MedicalParameter> parameters = localDocumentParser.parseLabParameters(extractedText.get());
                if (!parameters.isEmpty()) {
                    PrivacyService.PrivacyResult privacy = privacyService.scanAndRedact(extractedText.get());
                    return buildResponse(id, DocumentType.LAB_REPORT, "Blood Test Report",
                            parameters, privacy, false, lang, extractedText.get());
                }
            }

            if (!geminiService.isAvailable()) {
                throw new ArogyaLensException(
                        "AI_NOT_CONFIGURED",
                        "Gemini API key missing",
                        "Image analysis needs a Gemini API key. Add GEMINI_API_KEY to the project .env file and restart the backend. PDF text reports can still be analyzed without AI."
                );
            }

            throw new ArogyaLensException(
                    "AI_EMPTY",
                    "Could not read document",
                    "We couldn't confidently read this document. Try a clearer image or a text-based PDF with the full page visible."
            );
        } catch (ArogyaLensException ex) {
            throw ex;
        } catch (Exception e) {
            log.warn("Document analysis failed: {}", e.getClass().getSimpleName());
            throw new ArogyaLensException(
                    "ANALYSIS_FAILED",
                    "Analysis failed",
                    "We couldn't analyze this file. Please try another clearer JPG, PNG, or PDF."
            );
        }
    }

    private AnalysisResponse fromAi(String id, String json, String lang) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        PrivacyService.PrivacyResult summaryPrivacy = privacyService.scanAndRedact(
                root.path("rawTextSummary").asText("")
        );
        List<MedicalParameter> parameters = parseParameters(root.path("parameters"));
        DocumentType type = parseType(root.path("documentType").asText("LAB_REPORT"));
        String label = root.path("documentLabel").asText("Medical Document");
        return buildResponse(id, type, label, parameters, summaryPrivacy, true, lang, json);
    }

    private AnalysisResponse buildResponse(String id,
                                           DocumentType type,
                                           String label,
                                           List<MedicalParameter> parameters,
                                           PrivacyService.PrivacyResult privacy,
                                           boolean aiUsed,
                                           String lang,
                                           String context) {
        List<String> questions = doctorQuestionService.fromJsonOrDefault(null, parameters);
        DashboardSummaryDto dashboard = summarize(parameters);
        List<String> safetyNotes = new ArrayList<>();
        safetyNotes.add("⚠️ Important: This information may require discussion with a qualified healthcare professional.");
        safetyNotes.add("ArogyaLens cannot diagnose your condition or determine treatment from this document alone.");
        if (privacy.redacted()) {
            safetyNotes.add("Common personal identifiers were masked before processing where detected.");
        }

        Map<String, String> translations = offlineLanguagePack.all("hba1c");

        AnalysisResponse response = new AnalysisResponse(
                id,
                type,
                label,
                false,
                new PrivacyShieldDto(
                        privacy.findings(),
                        "ArogyaLens automatically detects and masks common personal identifiers before processing.",
                        privacy.redacted()
                ),
                steps(),
                1,
                parameters.size(),
                parameters,
                dashboard,
                questions,
                sourceService.forTopic(parameters.isEmpty() ? "lab" : parameters.getFirst().name()),
                safetyNotes,
                null,
                null,
                null,
                buildFamilySummary(dashboard, questions),
                translations,
                "Original document retained for verification. Files are processed temporarily and not stored permanently by default.",
                aiUsed,
                "We don't replace the doctor. We make healthcare easier to understand."
        );
        sessionService.save(id, response, context == null ? buildContext(response) : context);
        return response;
    }

    private String resolveMime(MultipartFile file) {
        String type = file.getContentType();
        if (type != null && !type.isBlank()) {
            return type;
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".pdf")) return "application/pdf";
        return "image/jpeg";
    }

    private List<MedicalParameter> parseParameters(JsonNode array) {
        List<MedicalParameter> list = new ArrayList<>();
        if (array == null || !array.isArray()) {
            return list;
        }
        for (JsonNode n : array) {
            double confidence = n.path("confidence").asDouble(0.5);
            String value = n.path("value").isNull() || n.path("value").asText().isBlank()
                    ? "Unable to confidently read this value. Please verify the original document."
                    : n.path("value").asText();
            ParameterStatus status = confidence < 0.7
                    ? ParameterStatus.LOW_CONFIDENCE
                    : parseStatus(n.path("status").asText("UNKNOWN"));
            String explanation = safetyValidationService.enforceSafeWording(
                    n.path("explanation").asText("I couldn't confidently determine this from the uploaded document.")
            );
            String simple = safetyValidationService.enforceSafeWording(
                    n.path("simpleExplanation").asText(explanation)
            );
            list.add(new MedicalParameter(
                    n.path("name").asText("Unknown parameter"),
                    value,
                    n.path("unit").asText(null),
                    n.path("referenceRange").asText(null),
                    status,
                    explanation,
                    simple,
                    confidence,
                    confidence < 0.7
            ));
        }
        return list;
    }

    private ParameterStatus parseStatus(String raw) {
        try {
            return ParameterStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return ParameterStatus.UNKNOWN;
        }
    }

    private DocumentType parseType(String raw) {
        try {
            return DocumentType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return DocumentType.UNKNOWN;
        }
    }

    private DashboardSummaryDto summarize(List<MedicalParameter> parameters) {
        int within = 0, discuss = 0, important = 0;
        for (MedicalParameter p : parameters) {
            switch (p.status()) {
                case WITHIN_RANGE -> within++;
                case IMPORTANT_ATTENTION -> important++;
                case OUTSIDE_RANGE, REQUIRES_DISCUSSION, UNKNOWN, LOW_CONFIDENCE -> discuss++;
            }
        }
        return new DashboardSummaryDto(parameters.size(), within, discuss, important);
    }

    private List<ProcessingStepDto> steps() {
        return List.of(
                new ProcessingStepDto("privacy", "Protecting personal information...", "done"),
                new ProcessingStepDto("understand", "Understanding document...", "done"),
                new ProcessingStepDto("extract", "Extracting medical information...", "done"),
                new ProcessingStepDto("simplify", "Simplifying medical terminology...", "done"),
                new ProcessingStepDto("translate", "Preparing multilingual explanation...", "done"),
                new ProcessingStepDto("sources", "Finding trusted sources...", "done"),
                new ProcessingStepDto("safety", "Running safety check...", "done")
        );
    }

    private String buildFamilySummary(DashboardSummaryDto dashboard, List<String> questions) {
        StringBuilder sb = new StringBuilder();
        sb.append("FAMILY SUMMARY\n\n");
        sb.append("The report contains results that may be worth discussing with a doctor.\n");
        sb.append("Within provided range: ").append(dashboard.withinRange()).append('\n');
        sb.append("Worth discussing: ").append(dashboard.needsDiscussion()).append('\n');
        sb.append("Important attention: ").append(dashboard.importantAttention()).append('\n');
        sb.append("\nThe report does not by itself establish a diagnosis.\n\nQuestions to ask:\n");
        int i = 1;
        for (String q : questions) {
            sb.append(i++).append(". ").append(q).append('\n');
        }
        return sb.toString();
    }

    private String buildContext(AnalysisResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("Document type: ").append(response.documentType()).append('\n');
        if (response.parameters() != null) {
            for (MedicalParameter p : response.parameters()) {
                sb.append(p.name()).append("=").append(p.value()).append(' ')
                        .append(p.unit() == null ? "" : p.unit())
                        .append(" status=").append(p.status()).append('\n');
            }
        }
        return sb.toString();
    }
}
