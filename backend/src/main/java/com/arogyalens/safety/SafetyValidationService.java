package com.arogyalens.safety;

import com.arogyalens.dto.SafetyValidateResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Safety layer: no diagnosis claims, no medication changes, emergency awareness. */
@Service
public class SafetyValidationService {

    private static final List<Pattern> DIAGNOSIS_PATTERNS = List.of(
            Pattern.compile("(?i)\\byou have (diabetes|cancer|hypertension|thyroid disease|anemia)\\b"),
            Pattern.compile("(?i)\\byou are (diabetic|hypertensive|anemic)\\b"),
            Pattern.compile("(?i)\\bthis (confirms|proves|means) you have\\b"),
            Pattern.compile("(?i)\\bdiagnosed with\\b")
    );

    private static final List<Pattern> PRESCRIPTION_PATTERNS = List.of(
            Pattern.compile("(?i)\\b(increase|decrease|raise|lower) (your )?(dose|dosage)\\b"),
            Pattern.compile("(?i)\\b(stop|start|discontinue|begin) (taking |using )?(this |the )?(medicine|medication|drug|tablet)\\b"),
            Pattern.compile("(?i)\\byou should take\\b"),
            Pattern.compile("(?i)\\breplace (this|your) (medicine|medication)\\b"),
            Pattern.compile("(?i)\\bprescrib(e|ed|ing)\\b")
    );

    private static final List<Pattern> EMERGENCY_PATTERNS = List.of(
            Pattern.compile("(?i)\\b(chest pain|difficulty breathing|severe bleeding|suicidal|stroke|heart attack)\\b"),
            Pattern.compile("(?i)\\bseek (immediate |emergency )?(care|help|attention)\\b")
    );

    private static final Pattern RED_FLAGS = Pattern.compile(
            "(?i)\\b(unconscious|not breathing|can't breathe|cannot breathe|seizure|fainted|severe burn|poison(ed|ing)?|overdose|"
                    + "slurred speech|face drooping|coughing blood|vomiting blood|suicide|kill myself|self[- ]harm)\\b");

    /**
     * Checks text for diagnosis claims and medication instructions, rewriting them into safe wording.
     */
    public SafetyValidateResponse validate(String text) {
        if (text == null || text.isBlank()) {
            return new SafetyValidateResponse(true, text, List.of(), List.of());
        }

        List<String> violations = new ArrayList<>();
        String sanitized = text;

        for (Pattern p : DIAGNOSIS_PATTERNS) {
            if (p.matcher(sanitized).find()) {
                violations.add("DIAGNOSIS_CLAIM");
                sanitized = p.matcher(sanitized).replaceAll(
                        "this result can be associated with a health concern that a healthcare professional should interpret"
                );
            }
        }

        for (Pattern p : PRESCRIPTION_PATTERNS) {
            if (p.matcher(sanitized).find()) {
                violations.add("MEDICATION_INSTRUCTION");
                sanitized = p.matcher(sanitized).replaceAll(
                        "discuss medication decisions with your healthcare professional"
                );
            }
        }

        List<String> notes = new ArrayList<>();
        if (!violations.isEmpty()) {
            notes.add("⚠️ Important: ArogyaLens cannot diagnose your condition or determine treatment from this document alone.");
            notes.add("This information may require discussion with a qualified healthcare professional.");
        }

        String lower = sanitized.toLowerCase(Locale.ROOT);
        for (Pattern p : EMERGENCY_PATTERNS) {
            if (p.matcher(lower).find()) {
                notes.add("If you are experiencing an emergency, seek appropriate professional care immediately.");
                break;
            }
        }

        boolean safe = violations.isEmpty();
        return new SafetyValidateResponse(safe, sanitized, violations.stream().distinct().toList(), notes);
    }

    /** @return true when the text mentions a red-flag emergency (chest pain, stroke, etc.) */
    public boolean isEmergency(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return EMERGENCY_PATTERNS.getFirst().matcher(lower).find()
                || RED_FLAGS.matcher(lower).find();
    }

    /** @return the text with diagnosis claims and medication instructions rewritten safely */
    public String enforceSafeWording(String text) {
        return validate(text).sanitizedText();
    }
}
