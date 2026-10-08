package com.arogyalens.exception;

public class ArogyaLensException extends RuntimeException {
    private final String code;
    private final String userMessage;

    public ArogyaLensException(String code, String message, String userMessage) {
        super(message);
        this.code = code;
        this.userMessage = userMessage;
    }

    public String getCode() {
        return code;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
