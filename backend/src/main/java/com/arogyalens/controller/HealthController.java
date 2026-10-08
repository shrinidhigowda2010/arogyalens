package com.arogyalens.controller;

import com.arogyalens.ai.GeminiService;
import com.arogyalens.config.ArogyaLensProperties;
import com.arogyalens.util.LanguageUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final GeminiService geminiService;
    private final ArogyaLensProperties properties;

    public HealthController(GeminiService geminiService, ArogyaLensProperties properties) {
        this.geminiService = geminiService;
        this.properties = properties;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "product", "ArogyaLens",
                "aiConfigured", geminiService.isAvailable(),
                "demoEnabled", properties.demo().enabled(),
                "languages", LanguageUtil.LANGUAGE_NAMES
        );
    }
}
