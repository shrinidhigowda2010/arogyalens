package com.arogyalens.controller;

import com.arogyalens.dto.AnalysisResponse;
import com.arogyalens.history.HistoryController;
import com.arogyalens.history.HistoryService;
import com.arogyalens.service.MedicineService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Medicine package scanning with general, non-prescriptive information. */
@RestController
@RequestMapping("/api/medicines")
public class MedicineController {

    private final MedicineService medicineService;
    private final HistoryService historyService;

    public MedicineController(MedicineService medicineService, HistoryService historyService) {
        this.medicineService = medicineService;
        this.historyService = historyService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalysisResponse analyze(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "demo", defaultValue = "false") boolean demo,
            @RequestParam(value = "language", defaultValue = "en") String language,
            @RequestHeader(value = HistoryController.DEVICE_HEADER, required = false)
                    String deviceId) {
        AnalysisResponse response = medicineService.analyze(file, demo);
        historyService.recordScan(deviceId, response, language);
        return response;
    }
}
