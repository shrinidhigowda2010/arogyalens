package com.arogyalens.service;

import com.arogyalens.ai.AiClient;
import com.arogyalens.ai.PromptLibrary;
import com.arogyalens.dto.TranslateRequest;
import com.arogyalens.dto.TranslateResponse;
import com.arogyalens.util.LanguageUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/** Translates text with Gemini, using the offline language pack as a fallback. */
@Service
public class TranslationService {

    private final AiClient aiClient;
    private final LanguageUtil languageUtil;
    private final ObjectMapper objectMapper;
    private final OfflineLanguagePack offlineLanguagePack;

    /** Per-model limit for a translation before the next fallback model is tried. */
    static final Duration MODEL_TIMEOUT = Duration.ofSeconds(20);

    /** The same vowel sign or mark twice in a row (e.g. "ೆೆ") is never valid in Indic scripts. */
    private static final Pattern DUPLICATE_MARK = Pattern.compile("(\\p{M})\\1+");

    public TranslationService(
            AiClient aiClient,
            LanguageUtil languageUtil,
            ObjectMapper objectMapper,
            OfflineLanguagePack offlineLanguagePack) {
        this.aiClient = aiClient;
        this.languageUtil = languageUtil;
        this.objectMapper = objectMapper;
        this.offlineLanguagePack = offlineLanguagePack;
    }

    public TranslateResponse translate(TranslateRequest request) {
        String lang = languageUtil.normalize(request.targetLanguage());
        String term = request.medicalTerm() == null ? "" : request.medicalTerm().toLowerCase();
        String source = request.text() == null ? "" : request.text();

        if (term.contains("hba1c") || source.toLowerCase().contains("hba1c")) {
            Map<String, String> all = offlineLanguagePack.all("hba1c");
            return new TranslateResponse(
                    source,
                    lang,
                    all.getOrDefault(lang, all.get("en")),
                    all,
                    request.medicalTerm(),
                    all.get("en"));
        }

        String offline = offlineLanguagePack.translateMedicalBlurb(source, lang);
        if (!"en".equals(lang) && !offline.equals(source)) {
            Map<String, String> all = new LinkedHashMap<>();
            all.put("en", source);
            all.put(lang, offline);
            return new TranslateResponse(source, lang, offline, all, request.medicalTerm(), source);
        }

        if ("en".equals(lang)) {
            return new TranslateResponse(
                    source, lang, source, Map.of("en", source), request.medicalTerm(), source);
        }

        String translated =
                aiClient.generateText(PromptLibrary.translationPrompt(lang, source), MODEL_TIMEOUT)
                        .map(this::readTranslated)
                        .map(TranslationService::fixDuplicateSigns)
                        .orElse(source);

        Map<String, String> all = new LinkedHashMap<>();
        all.put("en", source);
        all.put(lang, translated);
        return new TranslateResponse(source, lang, translated, all, request.medicalTerm(), source);
    }

    /** Removes repeated combining marks that models sometimes emit (e.g. ಮಾತ್ರೆೆ → ಮಾತ್ರೆ). */
    static String fixDuplicateSigns(String text) {
        return DUPLICATE_MARK.matcher(text).replaceAll("$1");
    }

    private String readTranslated(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return node.path("translated").asText(json);
        } catch (JsonProcessingException e) {
            // the model replied with plain text instead of JSON: use it as-is
            return json;
        }
    }
}
