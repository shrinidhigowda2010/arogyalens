package com.arogyalens.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "arogyalens")
public record ArogyaLensProperties(
        Cors cors,
        Ai ai,
        Files files,
        Languages languages,
        Demo demo,
        Features features,
        Privacy privacy,
        Session session
) {
    public record Cors(String allowedOrigins) {
        public List<String> origins() {
            if (allowedOrigins == null || allowedOrigins.isBlank()) {
                return List.of("http://localhost:5173");
            }
            return List.of(allowedOrigins.split(","));
        }
    }

    public record Ai(String provider, String apiKey, String model, long timeoutMs, boolean enabled) {
        public boolean isConfigured() {
            return enabled && apiKey != null && !apiKey.isBlank();
        }
    }

    public record Files(int maxSizeMb, String allowedTypes) {
        public List<String> allowedTypeList() {
            return List.of(allowedTypes.split(","));
        }
    }

    public record Languages(String supported, String defaultLanguage) {
        public List<String> supportedList() {
            return List.of(supported.split(","));
        }
    }

    public record Demo(boolean enabled) {}

    public record Features(boolean voice, boolean translation, boolean sources) {}

    public record Privacy(boolean redactBeforeAi) {}

    public record Session(int ttlMinutes) {}
}
