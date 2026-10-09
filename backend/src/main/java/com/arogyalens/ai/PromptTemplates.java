package com.arogyalens.ai;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompt templates stored as {@code classpath:prompts/<name>.txt} with {@code {{placeholder}}}
 * markers. Templates are read once, and rendering is a single pass over the template, so text
 * supplied by users can never be re-interpreted as a placeholder.
 */
public final class PromptTemplates {

    /** Every template shipped with the application. */
    public static final Set<String> NAMES =
            Set.of(
                    "system-safety",
                    "document-extraction",
                    "medicine",
                    "prescription",
                    "discharge",
                    "translation",
                    "doctor-questions",
                    "voice-assistant",
                    "specialty",
                    "safety-review");

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([a-zA-Z]+)}}");
    private static final Map<String, String> TEMPLATES = loadAll();

    private PromptTemplates() {}

    /** Returns the raw template text. */
    public static String raw(String name) {
        String template = TEMPLATES.get(name);
        if (template == null) {
            throw new IllegalArgumentException("Unknown prompt template: " + name);
        }
        return template;
    }

    /** Placeholder names used by a template. */
    public static Set<String> placeholders(String name) {
        Set<String> names = new TreeSet<>();
        Matcher m = PLACEHOLDER.matcher(raw(name));
        while (m.find()) {
            names.add(m.group(1));
        }
        return names;
    }

    /**
     * Fills every placeholder of a template.
     *
     * @throws IllegalArgumentException when a placeholder has no value
     */
    public static String render(String name, Map<String, String> values) {
        Matcher m = PLACEHOLDER.matcher(raw(name));
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String value = values.get(m.group(1));
            if (value == null) {
                throw new IllegalArgumentException(
                        "Missing value for {{" + m.group(1) + "}} in prompt " + name);
            }
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static Map<String, String> loadAll() {
        Map<String, String> map = new LinkedHashMap<>();
        for (String name : NAMES) {
            String path = "/prompts/" + name + ".txt";
            try (InputStream in = PromptTemplates.class.getResourceAsStream(path)) {
                if (in == null) {
                    throw new IllegalStateException(
                            "Prompt template missing on classpath: " + path);
                }
                map.put(name, new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read prompt template " + path, e);
            }
        }
        return Map.copyOf(map);
    }
}
