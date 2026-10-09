package com.arogyalens.ai;

import com.arogyalens.util.LanguageUtil;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds the Gemini prompts. Texts live in {@code src/main/resources/prompts/*.txt}; every task
 * prompt is prefixed with the shared safety rules ({@code system-safety.txt}).
 */
public final class PromptLibrary {

    /** Shared safety rules prepended to every prompt. */
    public static final String SYSTEM_SAFETY = PromptTemplates.raw("system-safety");

    private static final String AUTO_DETECT = "auto-detect";

    private PromptLibrary() {}

    private static String task(String name, Map<String, String> values) {
        return SYSTEM_SAFETY + PromptTemplates.render(name, values);
    }

    /** Extracts structured lab values from a medical document image or PDF. */
    public static String documentExtractionPrompt(String documentHint) {
        return task(
                "document-extraction",
                vars("hint", documentHint == null ? AUTO_DETECT : documentHint));
    }

    /** Identifies a medicine from its package and returns general information. */
    public static String medicinePrompt() {
        return task("medicine", vars());
    }

    /** Extracts prescription lines, flagging unclear handwriting. */
    public static String prescriptionPrompt() {
        return task("prescription", vars());
    }

    /** Extracts the sections of a discharge summary. */
    public static String dischargePrompt() {
        return task("discharge", vars());
    }

    /** Translates a medical explanation into the target language. */
    public static String translationPrompt(String targetLanguage, String text) {
        return task("translation", vars("language", targetLanguage, "text", text));
    }

    /** Generates non-diagnostic questions for a doctor visit from document context. */
    public static String doctorQuestionPrompt(String context) {
        return task("doctor-questions", vars("context", context));
    }

    /**
     * Prompt for the "ask anything" assistant. The user's question is fenced as data so
     * instructions inside it cannot override the safety rules (prompt-injection hardening).
     */
    public static String voiceAssistantPrompt(String context, String query, String language) {
        return task(
                "voice-assistant",
                vars("language", languageName(language), "context", context, "question", query));
    }

    /** Prompt that maps symptoms or a condition to a doctor specialty without diagnosing. */
    public static String specialtyPrompt(String condition, String language) {
        return task("specialty", vars("language", languageName(language), "concern", condition));
    }

    /** Rewrites text to remove diagnosis claims and medication-change instructions. */
    public static String safetyReviewPrompt(String text) {
        return task("safety-review", vars("text", text));
    }

    /** Key/value pairs; {@code null} values render as "null", as String.format did before. */
    private static Map<String, String> vars(String... keyValues) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], String.valueOf(keyValues[i + 1]));
        }
        return map;
    }

    static String languageName(String code) {
        String name = LanguageUtil.LANGUAGE_NAMES.get(code == null ? "en" : code);
        return name == null ? "English" : name + " (" + code + ")";
    }
}
