package com.arogyalens.controller;

import com.arogyalens.dto.DoctorQuestionsRequest;
import com.arogyalens.service.DoctorQuestionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctor-questions")
public class DoctorQuestionController {

    private final DoctorQuestionService doctorQuestionService;

    public DoctorQuestionController(DoctorQuestionService doctorQuestionService) {
        this.doctorQuestionService = doctorQuestionService;
    }

    @PostMapping
    public Map<String, List<String>> questions(@Valid @RequestBody DoctorQuestionsRequest request) {
        return Map.of("questions", doctorQuestionService.forSession(request.sessionId()));
    }
}
