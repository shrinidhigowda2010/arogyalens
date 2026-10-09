package com.arogyalens.doctor;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Request/response records for the doctor consultation finder. */
public final class DoctorDtos {

    private DoctorDtos() {}

    /** Symptoms or a condition (free text, PII-masked before AI) to map to a specialty. */
    public record SpecialtyRequest(
            @NotBlank @Size(max = 500) String condition,
            @Pattern(regexp = "^[a-z]{2}$") String language) {}

    public record SpecialtySuggestion(String specialty, String reason, boolean urgent, String source) {}

    /** Search near coordinates (from browser geolocation) or a typed city / PIN code. */
    public record SearchRequest(
            @NotBlank @Size(max = 60) @Pattern(regexp = "^[\\p{L} .&/()-]+$") String specialty,
            @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
            @Size(max = 100) @Pattern(regexp = "^[\\p{L}\\p{N} ,.-]*$") String location,
            @Pattern(regexp = "^[a-z]{2}$") String language) {}

    /** A real place returned by Google Places API (New). Never fabricated. */
    public record DoctorPlace(String id, String name, String address, Double rating, Integer ratingCount,
                              String phone, Boolean openNow, String mapsUrl) {}

    public record SearchLink(String label, String url, String description) {}

    public record SearchResponse(String specialty, String locationLabel, boolean placesEnabled,
                                 List<DoctorPlace> places, List<SearchLink> links, String notice) {}
}
