package com.arogyalens.support;

import com.arogyalens.config.ArogyaLensProperties;

/** Builds {@link ArogyaLensProperties} for unit tests without a Spring context. */
public final class TestProps {

    private TestProps() {}

    public static ArogyaLensProperties withAi(String apiKey, String model, String fallbacks) {
        return new ArogyaLensProperties(
                new ArogyaLensProperties.Cors("http://localhost:5173"),
                new ArogyaLensProperties.Ai("gemini", apiKey, model, fallbacks, "tts-model", 5_000, true),
                new ArogyaLensProperties.Cache(50, 60),
                new ArogyaLensProperties.RateLimit(20),
                new ArogyaLensProperties.Maps(""),
                new ArogyaLensProperties.Files(15, "image/jpeg,image/png,image/webp,application/pdf"),
                new ArogyaLensProperties.Languages("en,hi,kn,ta,te,mr,bn", "en"),
                new ArogyaLensProperties.Demo(true),
                new ArogyaLensProperties.Features(true, true, true),
                new ArogyaLensProperties.Privacy(true),
                new ArogyaLensProperties.Session(60));
    }

    public static ArogyaLensProperties defaults() {
        return withAi("", "gemini-flash-latest", "", false);
    }

    public static ArogyaLensProperties withAi(String apiKey, String model, String fallbacks, boolean enabled) {
        ArogyaLensProperties p = withAi(apiKey, model, fallbacks);
        return new ArogyaLensProperties(p.cors(),
                new ArogyaLensProperties.Ai("gemini", apiKey, model, fallbacks, "tts-model", 5_000, enabled),
                p.cache(), p.rateLimit(), p.maps(), p.files(), p.languages(), p.demo(), p.features(),
                p.privacy(), p.session());
    }
}
