package com.arogyalens.dto;

import java.time.Instant;

public record ApiError(String code, String message, String userMessage, Instant timestamp) {
    public static ApiError of(String code, String message, String userMessage) {
        return new ApiError(code, message, userMessage, Instant.now());
    }
}
