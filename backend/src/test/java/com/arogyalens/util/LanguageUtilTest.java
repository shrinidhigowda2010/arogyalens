package com.arogyalens.util;

import com.arogyalens.config.ArogyaLensProperties;
import com.arogyalens.exception.ArogyaLensException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LanguageUtilTest {

    private final LanguageUtil util = new LanguageUtil(new ArogyaLensProperties(
            new ArogyaLensProperties.Cors("http://localhost:5173"),
            new ArogyaLensProperties.Ai("gemini", "", "gemini-2.0-flash", 60000, false),
            new ArogyaLensProperties.Files(15, "image/jpeg,image/png,application/pdf"),
            new ArogyaLensProperties.Languages("en,hi,kn,ta,te,mr,bn", "en"),
            new ArogyaLensProperties.Demo(true),
            new ArogyaLensProperties.Features(true, true, true),
            new ArogyaLensProperties.Privacy(true),
            new ArogyaLensProperties.Session(60)
    ));

    @Test
    void defaultsToEnglish() {
        assertEquals("en", util.normalize(null));
    }

    @Test
    void acceptsKannada() {
        assertEquals("kn", util.normalize("KN"));
    }

    @Test
    void rejectsUnsupported() {
        assertThrows(ArogyaLensException.class, () -> util.normalize("fr"));
    }
}
