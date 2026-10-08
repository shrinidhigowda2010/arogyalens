package com.arogyalens.util;

import com.arogyalens.config.ArogyaLensProperties;
import com.arogyalens.exception.ArogyaLensException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileValidationUtilTest {

    private final FileValidationUtil util = new FileValidationUtil(new ArogyaLensProperties(
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
    void acceptsPng() {
        MockMultipartFile file = new MockMultipartFile("file", "report.png", "image/png", new byte[]{1, 2, 3});
        assertDoesNotThrow(() -> util.validate(file));
    }

    @Test
    void rejectsEmpty() {
        MockMultipartFile file = new MockMultipartFile("file", "report.png", "image/png", new byte[]{});
        assertThrows(ArogyaLensException.class, () -> util.validate(file));
    }
}
