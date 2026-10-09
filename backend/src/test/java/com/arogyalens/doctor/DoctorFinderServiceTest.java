package com.arogyalens.doctor;

import com.arogyalens.ai.GeminiService;
import com.arogyalens.doctor.DoctorDtos.SearchLink;
import com.arogyalens.doctor.DoctorDtos.SearchRequest;
import com.arogyalens.doctor.DoctorDtos.SearchResponse;
import com.arogyalens.doctor.DoctorDtos.SpecialtySuggestion;
import com.arogyalens.exception.ArogyaLensException;
import com.arogyalens.privacy.PrivacyService;
import com.arogyalens.safety.SafetyValidationService;
import com.arogyalens.support.TestProps;
import com.arogyalens.util.LanguageUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorFinderServiceTest {

    private final GeminiService gemini = mock(GeminiService.class);
    private final PlacesClient places = new PlacesClient(TestProps.defaults(), RestClient.builder());
    private final DoctorFinderService service = new DoctorFinderService(gemini, places, new PrivacyService(),
            new SafetyValidationService(), new LanguageUtil(TestProps.defaults()), new ObjectMapper());

    @Test
    void mapsKnownConcernsToSpecialtyWithoutCallingAi() {
        SpecialtySuggestion s = service.suggestSpecialty("high HbA1c and fasting glucose", "en");
        assertThat(s.specialty()).contains("Endocrinologist");
        assertThat(s.source()).isEqualTo("rules");
        verify(gemini, never()).generateText(anyString());
    }

    @Test
    void prefersSpecificSpecialtyOverGeneralPhysician() {
        assertThat(DoctorFinderService.matchRule("fever and skin rash")).contains("Dermatologist");
        assertThat(DoctorFinderService.matchRule("just a fever")).contains(DoctorFinderService.GENERAL);
    }

    @Test
    void usesAiForUnknownConcernsAndFlagsEmergencies() {
        when(gemini.generateText(anyString())).thenReturn(Optional.of(
                "{\"specialty\":\"Rheumatologist\",\"reason\":\"Handles autoimmune conditions.\",\"urgent\":false}"));
        SpecialtySuggestion s = service.suggestSpecialty("lupus follow-up", "en");
        assertThat(s.specialty()).isEqualTo("Rheumatologist");
        assertThat(s.source()).isEqualTo("ai");

        assertThat(service.suggestSpecialty("crushing chest pain", "en").urgent()).isTrue();
    }

    @Test
    void defaultsToGeneralPhysicianWhenAiUnavailable() {
        when(gemini.generateText(anyString())).thenReturn(Optional.empty());
        assertThat(service.suggestSpecialty("something unusual", "en").specialty())
                .isEqualTo(DoctorFinderService.GENERAL);
    }

    @Test
    void withoutMapsKeyReturnsDeepLinksAndNeverFakeDoctors() {
        SearchResponse res = service.search(new SearchRequest("Cardiologist", null, null, "Bengaluru", "en"));
        assertThat(res.placesEnabled()).isFalse();
        assertThat(res.places()).isEmpty();
        assertThat(res.links()).extracting(SearchLink::label)
                .containsExactly("Google Maps", "Practo", "eSanjeevani (Govt. of India)");
        assertThat(res.links().get(0).url()).startsWith("https://www.google.com/maps/search/?api=1&query=Cardiologist");
        assertThat(res.links().get(1).url()).isEqualTo("https://www.practo.com/bangalore/cardiologist");
    }

    @Test
    void coordinatesOrPinCodeWork() {
        SearchResponse res = service.search(new SearchRequest("Dentist", 12.97, 77.59, null, "en"));
        assertThat(res.links().get(0).url()).contains("12.97000%2C77.59000");
        SearchResponse pin = service.search(new SearchRequest("Dentist", null, null, "560001", "en"));
        assertThat(pin.links()).extracting(SearchLink::label).doesNotContain("Practo");
    }

    @Test
    void requiresSomeLocation() {
        assertThatThrownBy(() -> service.search(new SearchRequest("Dentist", null, null, " ", "en")))
                .isInstanceOf(ArogyaLensException.class)
                .extracting("code").isEqualTo("LOCATION_REQUIRED");
    }

    @Test
    void parsesPlacesApiResponse() throws Exception {
        var json = new ObjectMapper().readTree("""
                {"places":[{"id":"p1","displayName":{"text":"City Heart Clinic"},"formattedAddress":"MG Road",
                "rating":4.5,"userRatingCount":120,"nationalPhoneNumber":"080 1234 5678",
                "currentOpeningHours":{"openNow":true},"googleMapsUri":"https://maps.google.com/?cid=1"}]}""");
        var list = PlacesClient.parse(json);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).name()).isEqualTo("City Heart Clinic");
        assertThat(list.get(0).openNow()).isTrue();
        assertThat(list.get(0).phone()).isEqualTo("080 1234 5678");
        assertThat(PlacesClient.parse(null)).isEmpty();
    }
}
