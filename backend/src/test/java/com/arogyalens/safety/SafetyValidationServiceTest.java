package com.arogyalens.safety;

import com.arogyalens.dto.SafetyValidateResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafetyValidationServiceTest {

    private final SafetyValidationService service = new SafetyValidationService();

    @Test
    void blocksDiagnosisClaims() {
        SafetyValidateResponse response = service.validate("You have diabetes based on this report.");
        assertFalse(response.safe());
        assertTrue(response.violations().contains("DIAGNOSIS_CLAIM"));
        assertFalse(response.sanitizedText().toLowerCase().contains("you have diabetes"));
    }

    @Test
    void blocksMedicationInstructions() {
        SafetyValidateResponse response = service.validate("You should take this medicine and increase your dosage.");
        assertFalse(response.safe());
        assertTrue(response.violations().contains("MEDICATION_INSTRUCTION"));
    }
}
