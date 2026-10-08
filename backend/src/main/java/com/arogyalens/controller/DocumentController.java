package com.arogyalens.controller;

import com.arogyalens.dto.AnalysisResponse;
import com.arogyalens.service.DocumentAnalysisService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentAnalysisService documentAnalysisService;

    public DocumentController(DocumentAnalysisService documentAnalysisService) {
        this.documentAnalysisService = documentAnalysisService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalysisResponse analyze(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "demo", defaultValue = "false") boolean demo,
            @RequestParam(value = "hint", required = false) String hint,
            @RequestParam(value = "language", defaultValue = "en") String language
    ) {
        return documentAnalysisService.analyze(file, demo, hint, language);
    }
}
