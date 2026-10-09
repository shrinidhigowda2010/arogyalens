package com.arogyalens.exception;

import org.springframework.http.HttpStatus;

/**
 * Application error carrying a stable machine-readable {@code code}, an internal {@code message}
 * (never shown to users) and a friendly {@code userMessage}.
 */
public class ArogyaLensException extends RuntimeException {

    private final String code;
    private final String userMessage;
    private final HttpStatus status;

    public ArogyaLensException(String code, String message, String userMessage) {
        this(code, message, userMessage, HttpStatus.BAD_REQUEST);
    }

    public ArogyaLensException(String code, String message, String userMessage, HttpStatus status) {
        super(message);
        this.code = code;
        this.userMessage = userMessage;
        this.status = status == null ? HttpStatus.BAD_REQUEST : status;
    }

    public String getCode() {
        return code;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
