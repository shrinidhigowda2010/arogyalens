package com.arogyalens.controller;

import com.arogyalens.ai.AiClient;
import com.arogyalens.config.ArogyaLensProperties;
import com.arogyalens.history.HistoryService;
import com.arogyalens.util.LanguageUtil;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Liveness plus the feature flags the frontend needs at start-up. */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final AiClient aiClient;
    private final ArogyaLensProperties properties;
    private final HistoryService historyService;

    public HealthController(
            AiClient aiClient, ArogyaLensProperties properties, HistoryService historyService) {
        this.aiClient = aiClient;
        this.properties = properties;
        this.historyService = historyService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "product", "ArogyaLens",
                "aiConfigured", aiClient.isAvailable(),
                "demoEnabled", properties.demo().enabled(),
                "mapsConfigured", properties.maps().isConfigured(),
                "historyEnabled", historyService.isEnabled(),
                "languages", LanguageUtil.LANGUAGE_NAMES);
    }
}
