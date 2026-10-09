package com.arogyalens.doctor;

import com.arogyalens.doctor.DoctorDtos.SearchRequest;
import com.arogyalens.doctor.DoctorDtos.SearchResponse;
import com.arogyalens.doctor.DoctorDtos.SpecialtyRequest;
import com.arogyalens.doctor.DoctorDtos.SpecialtySuggestion;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Doctor consultation endpoints: specialty suggestion and nearby search. */
@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorFinderService service;

    public DoctorController(DoctorFinderService service) {
        this.service = service;
    }

    @PostMapping("/specialty")
    public SpecialtySuggestion specialty(@Valid @RequestBody SpecialtyRequest request) {
        return service.suggestSpecialty(request.condition(), request.language());
    }

    @PostMapping("/search")
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {
        return service.search(request);
    }
}
