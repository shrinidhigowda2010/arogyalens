package com.arogyalens.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PrivacyServiceTest {

    private final PrivacyService privacyService = new PrivacyService();

    @Test
    void detectsAndRedactsCommonPii() {
        String text =
                """
                Patient Name: Anita Sharma
                Phone: 9876543210
                Email: anita@example.com
                Patient ID: UHID-12345
                DOB: 12/05/1988
                Address: 42 MG Road Bengaluru Karnataka
                """;

        PrivacyService.PrivacyResult result = privacyService.scanAndRedact(text);

        assertThat(result.redacted()).isTrue();
        assertThat(result.findings())
                .extracting("type")
                .contains(
                        "Patient name",
                        "Phone number",
                        "Email",
                        "Patient ID",
                        "Date of birth",
                        "Address");
        assertThat(result.redactedText())
                .doesNotContain("9876543210")
                .doesNotContain("anita@example.com")
                .doesNotContain("Anita Sharma")
                .doesNotContain("UHID-12345");
    }

    @Test
    void masksAadhaarLikeNumbersAndInternationalPhones() {
        PrivacyService.PrivacyResult result =
                privacyService.scanAndRedact("ID 1234 5678 9012, call +91 9876543210");
        assertThat(result.redactedText())
                .doesNotContain("1234 5678 9012")
                .doesNotContain("9876543210");
    }

    @Test
    void leavesMedicalValuesUntouched() {
        PrivacyService.PrivacyResult result =
                privacyService.scanAndRedact("HbA1c 7.2 % (4.0 - 5.6)");
        assertThat(result.redacted()).isFalse();
        assertThat(result.redactedText()).isEqualTo("HbA1c 7.2 % (4.0 - 5.6)");
    }

    @Test
    void handlesNullAndBlank() {
        assertThat(privacyService.scanAndRedact(null).redactedText()).isEmpty();
        assertThat(privacyService.scanAndRedact("  ").redacted()).isFalse();
    }
}
