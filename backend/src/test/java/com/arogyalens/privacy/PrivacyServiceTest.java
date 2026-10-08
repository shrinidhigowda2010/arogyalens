package com.arogyalens.privacy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivacyServiceTest {

    private final PrivacyService privacyService = new PrivacyService();

    @Test
    void detectsAndRedactsCommonPii() {
        String text = """
                Patient Name: Anita Sharma
                Phone: 9876543210
                Email: anita@example.com
                Patient ID: UHID-12345
                DOB: 12/05/1988
                Address: 42 MG Road Bengaluru Karnataka
                """;

        PrivacyService.PrivacyResult result = privacyService.scanAndRedact(text);

        assertTrue(result.redacted());
        assertFalse(result.findings().isEmpty());
        assertFalse(result.redactedText().contains("9876543210"));
        assertFalse(result.redactedText().contains("anita@example.com"));
    }
}
