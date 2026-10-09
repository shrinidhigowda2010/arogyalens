package com.arogyalens.service;

import com.arogyalens.config.ArogyaLensProperties;
import com.arogyalens.dto.AnalysisResponse;
import com.arogyalens.exception.ArogyaLensException;
import com.arogyalens.safety.SafetyValidationService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Guards and helpers shared by the report, medicine, prescription and discharge scanners. */
@Component
public class ScanSupport {

    private final ArogyaLensProperties properties;
    private final SessionService sessionService;
    private final SafetyValidationService safetyValidationService;

    public ScanSupport(
            ArogyaLensProperties properties,
            SessionService sessionService,
            SafetyValidationService safetyValidationService) {
        this.properties = properties;
        this.sessionService = sessionService;
        this.safetyValidationService = safetyValidationService;
    }

    /**
     * Returns a stored demo analysis when {@code demo} is requested.
     *
     * @param demo whether the client asked for the sample document
     * @param builder builds the demo response for a new session id
     * @param context session context stored for follow-up questions
     * @param uploadHint user-facing hint shown when demo mode is disabled
     * @return the demo response, or empty when a real upload should be analyzed
     */
    public Optional<AnalysisResponse> demoIfRequested(
            boolean demo,
            Function<String, AnalysisResponse> builder,
            String context,
            String uploadHint) {
        if (!demo) {
            return Optional.empty();
        }
        if (!properties.demo().enabled()) {
            throw new ArogyaLensException(
                    "DEMO_DISABLED", "Demo disabled", "Demo mode is disabled. " + uploadHint);
        }
        String id = sessionService.createId();
        AnalysisResponse response = builder.apply(id);
        sessionService.save(id, response, context);
        return Optional.of(response);
    }

    /** Rejects a missing or empty upload with a friendly message. */
    public void requireFile(MultipartFile file, String userMessage) {
        if (file == null || file.isEmpty()) {
            throw new ArogyaLensException("EMPTY_FILE", "Empty upload", userMessage);
        }
    }

    /** Reads a JSON string array, passing every entry through the safety layer. */
    public List<String> safeList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(n -> list.add(safetyValidationService.enforceSafeWording(n.asText())));
        }
        return list;
    }

    /** Standard error for an upload that could not be read or parsed. */
    public static ArogyaLensException analysisFailed(String what, String userMessage) {
        return new ArogyaLensException("ANALYSIS_FAILED", what + " analysis failed", userMessage);
    }
}
