package com.arogyalens.doctor;

import com.arogyalens.ai.GeminiService;
import com.arogyalens.ai.PromptLibrary;
import com.arogyalens.doctor.DoctorDtos.SearchLink;
import com.arogyalens.doctor.DoctorDtos.SearchRequest;
import com.arogyalens.doctor.DoctorDtos.SearchResponse;
import com.arogyalens.doctor.DoctorDtos.SpecialtySuggestion;
import com.arogyalens.exception.ArogyaLensException;
import com.arogyalens.privacy.PrivacyService;
import com.arogyalens.safety.SafetyValidationService;
import com.arogyalens.util.LanguageUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Doctor consultation finder: maps a concern to a specialty (rules first, Gemini when unclear)
 * and finds real nearby doctors via Google Places, or deep links when no Maps key is configured.
 * It never invents doctors, phone numbers or ratings.
 */
@Service
public class DoctorFinderService {

    static final String GENERAL = "General Physician";

    /** Keyword to specialty rules, checked in order; cheap and quota-free. */
    private static final Map<String, String> RULES = new LinkedHashMap<>();

    static {
        rule(GENERAL, "fever", "cold", "cough", "body pain", "weakness", "fatigue");
        rule("Endocrinologist / Diabetologist", "diabet", "sugar", "glucose", "hba1c", "thyroid", "tsh", "insulin");
        rule("Cardiologist", "heart", "chest pain", "palpitation", "blood pressure", "hypertension", "cholesterol", "ecg");
        rule("Nephrologist", "kidney", "creatinine", "urea", "dialysis");
        rule("Gastroenterologist", "stomach", "liver", "acidity", "jaundice", "bilirubin", "sgpt", "alt ", "digest");
        rule("Pulmonologist", "breath", "asthma", "lung", "wheez");
        rule("Dermatologist", "skin", "rash", "acne", "itch", "eczema");
        rule("Orthopedist", "bone", "joint", "fracture", "back pain", "knee");
        rule("Neurologist", "headache", "migraine", "seizure", "numb", "stroke", "dizz");
        rule("Gynecologist", "pregnan", "period", "menstru", "pcos");
        rule("Pediatrician", "child", "baby", "infant");
        rule("Ophthalmologist", "eye", "vision");
        rule("ENT Specialist", "ear", "throat", "sinus", "tonsil");
        rule("Psychiatrist", "anxiety", "depress", "stress", "sleep");
        rule("Hematologist", "hemoglobin", "haemoglobin", "anemi", "anaemi", "platelet");
        rule("Urologist", "urine", "prostate", "bladder");
        rule("Dentist", "tooth", "teeth", "gum");
    }

    private static void rule(String specialty, String... keywords) {
        for (String k : keywords) {
            RULES.put(k, specialty);
        }
    }

    private final GeminiService geminiService;
    private final PlacesClient placesClient;
    private final PrivacyService privacyService;
    private final SafetyValidationService safetyValidationService;
    private final LanguageUtil languageUtil;
    private final ObjectMapper objectMapper;

    public DoctorFinderService(GeminiService geminiService, PlacesClient placesClient,
                               PrivacyService privacyService, SafetyValidationService safetyValidationService,
                               LanguageUtil languageUtil, ObjectMapper objectMapper) {
        this.geminiService = geminiService;
        this.placesClient = placesClient;
        this.privacyService = privacyService;
        this.safetyValidationService = safetyValidationService;
        this.languageUtil = languageUtil;
        this.objectMapper = objectMapper;
    }

    /** Suggests a specialty for a concern; flags red-flag emergencies. Never diagnoses. */
    public SpecialtySuggestion suggestSpecialty(String condition, String language) {
        String lang = languageUtil.normalize(language);
        boolean urgent = safetyValidationService.isEmergency(condition);
        Optional<String> ruled = matchRule(condition);
        if (ruled.isPresent()) {
            return new SpecialtySuggestion(ruled.get(),
                    "Based on the words you used, this kind of doctor commonly handles such concerns.",
                    urgent, "rules");
        }
        String masked = privacyService.scanAndRedact(condition).redactedText();
        Optional<String> ai = geminiService.generateText(PromptLibrary.specialtyPrompt(masked, lang));
        if (ai.isPresent()) {
            try {
                JsonNode root = objectMapper.readTree(ai.get());
                String specialty = root.path("specialty").asText("").strip();
                if (!specialty.isEmpty() && specialty.length() <= 60) {
                    return new SpecialtySuggestion(specialty,
                            safetyValidationService.enforceSafeWording(root.path("reason").asText("")),
                            urgent || root.path("urgent").asBoolean(false), "ai");
                }
            } catch (Exception e) {
                // fall through to general physician
            }
        }
        return new SpecialtySuggestion(GENERAL,
                "A general physician can assess this first and refer you to a specialist if needed.",
                urgent, "default");
    }

    static Optional<String> matchRule(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String lower = " " + text.toLowerCase(Locale.ROOT) + " ";
        // Prefer the most specific (non-general) match.
        String general = null;
        for (Map.Entry<String, String> e : RULES.entrySet()) {
            if (lower.contains(e.getKey())) {
                if (!GENERAL.equals(e.getValue())) {
                    return Optional.of(e.getValue());
                }
                general = e.getValue();
            }
        }
        return Optional.ofNullable(general);
    }

    /** Finds doctors of a specialty near coordinates or a typed city / PIN code. */
    public SearchResponse search(SearchRequest request) {
        String lang = languageUtil.normalize(request.language());
        boolean hasCoords = request.latitude() != null && request.longitude() != null;
        String location = request.location() == null ? "" : request.location().strip();
        if (!hasCoords && location.isEmpty()) {
            throw new ArogyaLensException("LOCATION_REQUIRED", "No location",
                    "Please allow location access or type your city or PIN code.");
        }
        String specialty = request.specialty().strip();
        String locationLabel = !location.isEmpty() ? location
                : String.format(Locale.ROOT, "%.3f, %.3f", request.latitude(), request.longitude());
        String query = specialty + (location.isEmpty() ? " near me" : " in " + location);

        List<DoctorDtos.DoctorPlace> places = placesClient.isConfigured()
                ? placesClient.search(query, request.latitude(), request.longitude(), lang)
                : List.of();

        String notice = placesClient.isConfigured()
                ? (places.isEmpty() ? "No results from Google Maps right now. Try the links below." : null)
                : "Live listings are not enabled on this server. These links open trusted search pages.";
        return new SearchResponse(specialty, locationLabel, placesClient.isConfigured(), places,
                links(specialty, location, request.latitude(), request.longitude()), notice);
    }

    static List<SearchLink> links(String specialty, String location, Double lat, Double lng) {
        List<SearchLink> links = new ArrayList<>();
        String mapsQuery = lat != null && lng != null && (location == null || location.isBlank())
                ? specialty + " near " + String.format(Locale.ROOT, "%.5f,%.5f", lat, lng)
                : specialty + " near " + location;
        links.add(new SearchLink("Google Maps",
                "https://www.google.com/maps/search/?api=1&query=" + enc(mapsQuery),
                "See nearby clinics, directions and phone numbers on Google Maps."));
        if (location != null && !location.isBlank() && !location.chars().allMatch(c -> Character.isDigit(c) || c == ' ')) {
            String city = location.split(",")[0].strip().toLowerCase(Locale.ROOT);
            city = PRACTO_CITY.getOrDefault(city, city).replace(' ', '-');
            String slug = specialty.split("[/(]")[0].strip().toLowerCase(Locale.ROOT).replace(' ', '-');
            links.add(new SearchLink("Practo",
                    "https://www.practo.com/" + enc(city) + "/" + enc(slug),
                    "Book in-clinic or video consultations."));
        }
        links.add(new SearchLink("eSanjeevani (Govt. of India)",
                "https://esanjeevani.mohfw.gov.in/",
                "Free national telemedicine service: consult a doctor online."));
        return links;
    }

    /** Practo uses legacy English city names in its URLs. */
    private static final Map<String, String> PRACTO_CITY = Map.of(
            "bengaluru", "bangalore", "mysuru", "mysore", "mangaluru", "mangalore",
            "new delhi", "delhi", "gurugram", "gurgaon", "kolkata", "kolkata", "belagavi", "belgaum");

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
