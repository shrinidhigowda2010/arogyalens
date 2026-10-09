package com.arogyalens.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed application configuration bound from the {@code arogyalens.*} namespace. Every nested
 * record tolerates missing values so a partial configuration never crashes the application context.
 */
@ConfigurationProperties(prefix = "arogyalens")
public record ArogyaLensProperties(
        Cors cors,
        Ai ai,
        Cache cache,
        RateLimit rateLimit,
        Maps maps,
        Files files,
        Languages languages,
        Demo demo,
        Features features,
        Privacy privacy,
        Session session) {

    public ArogyaLensProperties {
        cors = cors == null ? new Cors(null) : cors;
        ai =
                ai == null
                        ? new Ai("gemini", "", "gemini-flash-latest", null, null, 45_000, false)
                        : ai;
        cache = cache == null ? new Cache(200, 900) : cache;
        rateLimit = rateLimit == null ? new RateLimit(20) : rateLimit;
        maps = maps == null ? new Maps("") : maps;
        files =
                files == null
                        ? new Files(15, "image/jpeg,image/png,image/webp,application/pdf")
                        : files;
        languages = languages == null ? new Languages("en,hi,kn,ta,te,mr,bn", "en") : languages;
        demo = demo == null ? new Demo(false) : demo;
        features = features == null ? new Features(true, true, true) : features;
        privacy = privacy == null ? new Privacy(true) : privacy;
        session = session == null ? new Session(60) : session;
    }

    static List<String> splitCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(",")).map(String::strip).filter(s -> !s.isEmpty()).toList();
    }

    /** CORS settings; origins come from the ALLOWED_ORIGINS environment variable. */
    public record Cors(String allowedOrigins) {
        public List<String> origins() {
            List<String> list = splitCsv(allowedOrigins);
            return list.isEmpty() ? List.of("http://localhost:5173") : list;
        }
    }

    /** Gemini settings. */
    public record Ai(
            String provider,
            String apiKey,
            String model,
            String fallbackModels,
            String ttsModel,
            long timeoutMs,
            boolean enabled) {
        public boolean isConfigured() {
            return enabled && apiKey != null && !apiKey.isBlank();
        }

        public List<String> fallbackModelList() {
            return splitCsv(fallbackModels);
        }
    }

    /** Short-lived cache of identical AI requests. */
    public record Cache(int maxEntries, int ttlSeconds) {}

    /** Per-client request budget for AI-backed endpoints. */
    public record RateLimit(int requestsPerMinute) {}

    /** Google Maps Platform (Places API New) settings for the doctor finder. */
    public record Maps(String apiKey) {
        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    public record Files(int maxSizeMb, String allowedTypes) {
        public List<String> allowedTypeList() {
            return splitCsv(allowedTypes);
        }
    }

    public record Languages(String supported, String defaultLanguage) {
        public List<String> supportedList() {
            return splitCsv(supported);
        }
    }

    public record Demo(boolean enabled) {}

    public record Features(boolean voice, boolean translation, boolean sources) {}

    public record Privacy(boolean redactBeforeAi) {}

    public record Session(int ttlMinutes) {}
}
