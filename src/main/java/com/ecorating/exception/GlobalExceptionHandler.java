package com.ecorating.exception;

import com.ecorating.dto.response.ApiResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalApiException(ExternalApiException ex) {
        log.warn("External API failure from {}: {}", ex.getSource(), ex.getMessage(), ex);
        String fallbackMessage = "External source " + ex.getSource() + " is temporarily unavailable. Fallback values were applied.";
        return ResponseEntity.ok(ApiResponse.fail(fallbackMessage));
    }

    @ExceptionHandler(AddressNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleAddressNotFoundException(AddressNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler(GeocodingException.class)
    public ResponseEntity<ApiResponse<Void>> handleGeocodingException(GeocodingException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        List<String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldErrorMessage)
                .toList();

        ApiResponse<List<String>> response = new ApiResponse<>(
                false,
                fieldErrors,
                "Validation failed",
                ApiResponse.fail("Validation failed").timestamp()
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("Internal server error"));
    }

    private String toFieldErrorMessage(FieldError fieldError) {
        String defaultMessage = fieldError.getDefaultMessage() == null ? "invalid value" : fieldError.getDefaultMessage();
        return fieldError.getField() + ": " + defaultMessage;
    }
}
