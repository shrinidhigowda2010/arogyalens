package com.arogyalens.controller;

import com.arogyalens.dto.AnalysisResponse;
import com.arogyalens.service.PrescriptionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalysisResponse analyze(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "demo", defaultValue = "false") boolean demo
    ) {
        return prescriptionService.analyze(file, demo);
    }
}
