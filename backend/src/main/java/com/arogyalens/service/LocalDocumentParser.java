package com.arogyalens.service;

import com.arogyalens.model.MedicalParameter;
import com.arogyalens.model.ParameterStatus;
import com.arogyalens.model.PrescriptionItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Extracts usable medical structure from PDFs / plain text without calling an AI provider.
 * Image-only uploads still require Gemini.
 */
@Component
public class LocalDocumentParser {

    private static final Pattern LAB_LINE =
            Pattern.compile(
                    "(?im)^\\s*([A-Za-z][A-Za-z0-9 \\-/%()]{1,40}?)\\s+([<>]?\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/%^0-9.\\-μu]*)\\s+([<>]?\\d+(?:\\.\\d+)?\\s*[-–to]+\\s*[<>]?\\d+(?:\\.\\d+)?|[<>]=?\\s*\\d+(?:\\.\\d+)?)");

    private static final Pattern RX_LINE =
            Pattern.compile(
                    "(?im)\\b([A-Za-z][A-Za-z0-9 \\-]{2,30})\\s+(\\d+(?:\\.\\d+)?\\s*mg)?\\s*(1-0-1|1-0-0|0-0-1|1-1-1|0-1-0)\\b(?:\\s*(after food|before food|bedtime))?");

    public Optional<String> extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Optional.empty();
        }
        String name =
                file.getOriginalFilename() == null
                        ? ""
                        : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        String type =
                file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        boolean pdf = type.contains("pdf") || name.endsWith(".pdf");
        if (!pdf) {
            return Optional.empty();
        }
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            if (text == null || text.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(text);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<MedicalParameter> parseLabParameters(String text) {
        List<MedicalParameter> list = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return list;
        }
        Matcher matcher = LAB_LINE.matcher(text);
        while (matcher.find()) {
            String name = matcher.group(1).trim().replaceAll("\\s+", " ");
            String value = matcher.group(2).trim();
            String unit = matcher.group(3) == null ? "" : matcher.group(3).trim();
            String range = matcher.group(4).trim().replace('–', '-');
            ParameterStatus status = compareToRange(value, range);
            list.add(
                    new MedicalParameter(
                            name,
                            value,
                            unit.isBlank() ? null : unit,
                            range,
                            status,
                            explanationFor(name, status),
                            simpleFor(name, status),
                            0.82,
                            false));
            if (list.size() >= 40) {
                break;
            }
        }
        return list;
    }

    public List<PrescriptionItem> parsePrescription(String text) {
        List<PrescriptionItem> items = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return items;
        }
        Matcher matcher = RX_LINE.matcher(text);
        while (matcher.find()) {
            String freq = matcher.group(3);
            boolean morning = freq.startsWith("1");
            boolean afternoon = freq.length() >= 3 && freq.charAt(2) == '1';
            boolean night = freq.endsWith("1");
            items.add(
                    new PrescriptionItem(
                            matcher.group(1).trim(),
                            matcher.group(2) == null ? null : matcher.group(2).trim(),
                            freq,
                            timingLabel(morning, afternoon, night),
                            matcher.group(4) == null ? null : capitalize(matcher.group(4)),
                            morning,
                            afternoon,
                            night,
                            true,
                            null));
            if (items.size() >= 20) {
                break;
            }
        }
        return items;
    }

    private ParameterStatus compareToRange(String valueRaw, String rangeRaw) {
        try {
            double value = Double.parseDouble(valueRaw.replace("<", "").replace(">", ""));
            String range = rangeRaw.replace(" ", "");
            if (range.startsWith("<") || range.startsWith("<=")) {
                double max = Double.parseDouble(range.replace("<", "").replace("=", ""));
                return value <= max
                        ? ParameterStatus.WITHIN_RANGE
                        : ParameterStatus.REQUIRES_DISCUSSION;
            }
            if (range.startsWith(">") || range.startsWith(">=")) {
                double min = Double.parseDouble(range.replace(">", "").replace("=", ""));
                return value >= min
                        ? ParameterStatus.WITHIN_RANGE
                        : ParameterStatus.REQUIRES_DISCUSSION;
            }
            String[] parts = range.split("[-–]|to");
            if (parts.length >= 2) {
                double min = Double.parseDouble(parts[0].replace("<", "").replace(">", ""));
                double max = Double.parseDouble(parts[1].replace("<", "").replace(">", ""));
                if (value < min || value > max) {
                    return value < min * 0.7 || value > max * 1.4
                            ? ParameterStatus.IMPORTANT_ATTENTION
                            : ParameterStatus.OUTSIDE_RANGE;
                }
                return ParameterStatus.WITHIN_RANGE;
            }
        } catch (Exception ignored) {
            return ParameterStatus.UNKNOWN;
        }
        return ParameterStatus.UNKNOWN;
    }

    private String explanationFor(String name, ParameterStatus status) {
        return switch (status) {
            case WITHIN_RANGE -> name + " is within the reference range shown on your document.";
            case IMPORTANT_ATTENTION ->
                    name
                            + " is far from the reference range shown. Discuss this with a healthcare professional.";
            case OUTSIDE_RANGE, REQUIRES_DISCUSSION ->
                    name
                            + " is outside the reference range shown on your document. Discuss it with your healthcare professional.";
            default ->
                    "I couldn't confidently interpret "
                            + name
                            + " from the uploaded document alone.";
        };
    }

    private String simpleFor(String name, ParameterStatus status) {
        return switch (status) {
            case WITHIN_RANGE -> name + " looks inside the usual range printed on the report.";
            case IMPORTANT_ATTENTION ->
                    name + " looks quite different from the usual range. Please ask your doctor.";
            case OUTSIDE_RANGE, REQUIRES_DISCUSSION ->
                    name
                            + " is not inside the range printed here, so it is worth asking your doctor.";
            default -> "Please check the original report for " + name + ".";
        };
    }

    private String timingLabel(boolean morning, boolean afternoon, boolean night) {
        List<String> parts = new ArrayList<>();
        if (morning) {
            parts.add("Morning");
        }
        if (afternoon) {
            parts.add("Afternoon");
        }
        if (night) {
            parts.add("Night");
        }
        return String.join(" and ", parts);
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.ROOT);
    }
}
