package com.arogyalens.controller;

import com.arogyalens.dto.SafetyValidateRequest;
import com.arogyalens.dto.SafetyValidateResponse;
import com.arogyalens.safety.SafetyValidationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/safety")
public class SafetyController {

    private final SafetyValidationService safetyValidationService;

    public SafetyController(SafetyValidationService safetyValidationService) {
        this.safetyValidationService = safetyValidationService;
    }

    @PostMapping("/validate")
    public SafetyValidateResponse validate(@Valid @RequestBody SafetyValidateRequest request) {
        return safetyValidationService.validate(request.text());
    }
}
