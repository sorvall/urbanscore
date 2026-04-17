package com.ecorating.exception;

public class ExternalApiException extends RuntimeException {

    private final String source;

    public ExternalApiException(String source, String message, Throwable cause) {
        super(message, cause);
        this.source = source;
    }

    public String getSource() {
        return source;
    }
}
