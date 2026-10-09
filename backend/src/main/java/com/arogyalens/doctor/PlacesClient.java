package com.arogyalens.doctor;

import com.arogyalens.config.ArogyaLensProperties;
import com.arogyalens.doctor.DoctorDtos.DoctorPlace;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Google Places API (New) Text Search client. The API key stays on the server and is sent in the
 * {@code X-Goog-Api-Key} header; only the fields we display are requested (field mask).
 */
@Component
public class PlacesClient {

    static final String URL = "https://places.googleapis.com/v1/places:searchText";
    static final String FIELD_MASK =
            String.join(
                    ",",
                    "places.id",
                    "places.displayName",
                    "places.formattedAddress",
                    "places.rating",
                    "places.userRatingCount",
                    "places.nationalPhoneNumber",
                    "places.internationalPhoneNumber",
                    "places.currentOpeningHours.openNow",
                    "places.googleMapsUri");
    private static final Logger LOG = LoggerFactory.getLogger(PlacesClient.class);

    private final ArogyaLensProperties properties;
    private final RestClient restClient;

    public PlacesClient(ArogyaLensProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.build();
    }

    public boolean isConfigured() {
        return properties.maps().isConfigured();
    }

    /**
     * @return real places matching the query, or an empty list if the API is unavailable
     */
    public List<DoctorPlace> search(String textQuery, Double lat, Double lng, String language) {
        if (!isConfigured()) {
            return List.of();
        }
        Map<String, Object> body = new HashMap<>();
        body.put("textQuery", textQuery);
        body.put("languageCode", language == null ? "en" : language);
        body.put("regionCode", "IN");
        body.put("pageSize", 8);
        if (lat != null && lng != null) {
            body.put(
                    "locationBias",
                    Map.of(
                            "circle",
                            Map.of(
                                    "center",
                                    Map.of("latitude", lat, "longitude", lng),
                                    "radius",
                                    10_000.0)));
        }
        try {
            JsonNode root =
                    restClient
                            .post()
                            .uri(URL)
                            .header("X-Goog-Api-Key", properties.maps().apiKey().strip())
                            .header("X-Goog-FieldMask", FIELD_MASK)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(body)
                            .retrieve()
                            .body(JsonNode.class);
            return parse(root);
        } catch (RestClientException e) {
            LOG.warn("Places search failed: {}", e.getClass().getSimpleName());
            return List.of();
        }
    }

    static List<DoctorPlace> parse(JsonNode root) {
        List<DoctorPlace> places = new ArrayList<>();
        if (root == null) {
            return places;
        }
        for (JsonNode p : root.path("places")) {
            String phone =
                    p.path("nationalPhoneNumber")
                            .asText(p.path("internationalPhoneNumber").asText(null));
            JsonNode open = p.path("currentOpeningHours").path("openNow");
            places.add(
                    new DoctorPlace(
                            p.path("id").asText(null),
                            p.path("displayName").path("text").asText("Unnamed place"),
                            p.path("formattedAddress").asText(null),
                            p.hasNonNull("rating") ? p.get("rating").asDouble() : null,
                            p.hasNonNull("userRatingCount")
                                    ? p.get("userRatingCount").asInt()
                                    : null,
                            phone,
                            open.isBoolean() ? open.asBoolean() : null,
                            p.path("googleMapsUri").asText(null)));
        }
        return places;
    }
}
