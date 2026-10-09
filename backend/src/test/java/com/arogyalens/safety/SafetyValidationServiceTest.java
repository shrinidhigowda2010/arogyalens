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

    @Test
    void safeTextPassesUnchanged() {
        SafetyValidateResponse response = service.validate("HbA1c reflects average blood sugar over about three months.");
        assertTrue(response.safe());
        assertTrue(response.violations().isEmpty());
    }

    @Test
    void detectsEmergencyRedFlags() {
        assertTrue(service.isEmergency("My father has chest pain and is sweating"));
        assertTrue(service.isEmergency("she fainted and is unconscious"));
        assertFalse(service.isEmergency("what is a normal cholesterol level"));
        assertFalse(service.isEmergency(null));
    }

    @Test
    void emergencyTextAddsCareNote() {
        SafetyValidateResponse response = service.validate("If you have chest pain, seek emergency care.");
        assertTrue(response.notes().stream().anyMatch(n -> n.contains("emergency")));
    }
}
