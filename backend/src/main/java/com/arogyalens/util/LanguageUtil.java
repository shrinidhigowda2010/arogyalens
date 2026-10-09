package com.arogyalens.util;

import com.arogyalens.config.ArogyaLensProperties;
import com.arogyalens.exception.ArogyaLensException;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Supported language codes and their display names. */
@Component
public class LanguageUtil {

    public static final Map<String, String> LANGUAGE_NAMES =
            Map.of(
                    "en", "English",
                    "hi", "Hindi",
                    "kn", "Kannada",
                    "ta", "Tamil",
                    "te", "Telugu",
                    "mr", "Marathi",
                    "bn", "Bengali");

    private final ArogyaLensProperties properties;

    public LanguageUtil(ArogyaLensProperties properties) {
        this.properties = properties;
    }

    public String normalize(String language) {
        if (language == null || language.isBlank()) {
            return properties.languages().defaultLanguage();
        }
        String code = language.trim().toLowerCase(Locale.ROOT);
        if (!properties.languages().supportedList().contains(code)) {
            throw new ArogyaLensException(
                    "UNSUPPORTED_LANGUAGE",
                    "Unsupported language: " + language,
                    "That language is not supported yet. Please choose English, Hindi, Kannada, Tamil, Telugu, Marathi, or Bengali.");
        }
        return code;
    }

    public boolean isSupported(String language) {
        return language != null
                && properties
                        .languages()
                        .supportedList()
                        .contains(language.toLowerCase(Locale.ROOT));
    }
}
