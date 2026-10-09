package com.arogyalens.privacy;

import com.arogyalens.model.PiiFinding;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class PrivacyService {

    private static final List<PatternRule> RULES =
            List.of(
                    new PatternRule(
                            "Patient name",
                            Pattern.compile(
                                    "(?i)\\b(?:patient\\s*name|name)\\s*[:\\-]?\\s*([A-Z][a-z]+(?:\\s+[A-Z][a-z]+){0,3})")),
                    new PatternRule(
                            "Phone number",
                            Pattern.compile("(?<!\\d)(?:\\+91[\\s-]?)?[6-9]\\d{9}(?!\\d)")),
                    new PatternRule(
                            "Email",
                            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")),
                    new PatternRule(
                            "Aadhaar-like ID",
                            Pattern.compile("(?<!\\d)\\d{4}\\s\\d{4}\\s\\d{4}(?!\\d)")),
                    new PatternRule(
                            "Patient ID",
                            Pattern.compile(
                                    "(?i)\\b(?:patient\\s*id|uhid|mrn|reg(?:istration)?\\s*(?:no|number)?)\\s*[:\\-]?\\s*([A-Z0-9\\-/]{4,})")),
                    new PatternRule(
                            "Date of birth",
                            Pattern.compile(
                                    "(?i)\\b(?:dob|date\\s*of\\s*birth)\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})")),
                    new PatternRule(
                            "Address",
                            Pattern.compile(
                                    "(?i)\\b(?:address|residence)\\s*[:\\-]?\\s*(.{10,80})")));

    public PrivacyResult scanAndRedact(String text) {
        if (text == null || text.isBlank()) {
            return new PrivacyResult(text == null ? "" : text, List.of(), false);
        }

        String redacted = text;
        List<PiiFinding> findings = new ArrayList<>();

        for (PatternRule rule : RULES) {
            Matcher matcher = rule.pattern().matcher(redacted);
            StringBuffer sb = new StringBuffer();
            boolean found = false;
            while (matcher.find()) {
                found = true;
                String original = matcher.group();
                String masked = mask(original);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
                findings.add(new PiiFinding(rule.label(), masked, true));
            }
            matcher.appendTail(sb);
            if (found) {
                redacted = sb.toString();
            }
        }

        return new PrivacyResult(redacted, findings, !findings.isEmpty());
    }

    private String mask(String value) {
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, Math.min(2, value.length()))
                + "****"
                + value.substring(Math.max(value.length() - 2, 2));
    }

    public record PrivacyResult(String redactedText, List<PiiFinding> findings, boolean redacted) {}

    private record PatternRule(String label, Pattern pattern) {}
}
