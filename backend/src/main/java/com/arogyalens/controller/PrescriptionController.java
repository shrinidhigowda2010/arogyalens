package com.arogyalens.controller;

import com.arogyalens.dto.AnalysisResponse;
import com.arogyalens.history.HistoryController;
import com.arogyalens.history.HistoryService;
import com.arogyalens.service.PrescriptionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final HistoryService historyService;

    public PrescriptionController(
            PrescriptionService prescriptionService, HistoryService historyService) {
        this.prescriptionService = prescriptionService;
        this.historyService = historyService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalysisResponse analyze(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "demo", defaultValue = "false") boolean demo,
            @RequestParam(value = "language", defaultValue = "en") String language,
            @RequestHeader(value = HistoryController.DEVICE_HEADER, required = false)
                    String deviceId) {
        AnalysisResponse response = prescriptionService.analyze(file, demo);
        historyService.recordScan(deviceId, response, language);
        return response;
    }
}
