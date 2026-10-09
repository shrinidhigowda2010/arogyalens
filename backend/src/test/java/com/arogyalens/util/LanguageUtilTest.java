package com.arogyalens.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.arogyalens.exception.ArogyaLensException;
import com.arogyalens.support.TestProps;
import org.junit.jupiter.api.Test;

class LanguageUtilTest {

    private final LanguageUtil util = new LanguageUtil(TestProps.defaults());

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
