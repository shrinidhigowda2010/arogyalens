package com.arogyalens.dto;

import java.time.Instant;

/** Stable error body: machine-readable code plus a user-safe message; never internals. */
public record ApiError(String code, String message, String userMessage, Instant timestamp) {
    public static ApiError of(String code, String message, String userMessage) {
        return new ApiError(code, message, userMessage, Instant.now());
    }
}
