package com.arogyalens.controller;

import com.arogyalens.dto.ChatRequest;
import com.arogyalens.dto.ChatResponse;
import com.arogyalens.dto.VoiceQueryRequest;
import com.arogyalens.dto.VoiceQueryResponse;
import com.arogyalens.service.VoiceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class VoiceController {

    private final VoiceService voiceService;

    public VoiceController(VoiceService voiceService) {
        this.voiceService = voiceService;
    }

    @PostMapping("/voice/query")
    public VoiceQueryResponse voice(@Valid @RequestBody VoiceQueryRequest request) {
        return voiceService.query(request);
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return voiceService.chat(request);
    }
}
