package com.arogyalens.exception;

import com.arogyalens.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ArogyaLensException.class)
    public ResponseEntity<ApiError> handleApp(ArogyaLensException ex) {
        log.warn("Handled application error: {}", ex.getCode());
        return ResponseEntity.badRequest().body(
                ApiError.of(ex.getCode(), ex.getMessage(), ex.getUserMessage())
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
                ApiError.of("FILE_TOO_LARGE", "Upload exceeds limit",
                        "The file is too large. Please upload a file under 15 MB.")
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(
                ApiError.of("VALIDATION_ERROR", "Invalid request",
                        "Please check your input and try again.")
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiError.of("INTERNAL_ERROR", "Unexpected error",
                        "Something went wrong while processing your request. Please try again.")
        );
    }
}
