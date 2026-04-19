package com.ecorating.dto;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record ApiResponse<T>(boolean success, T data, String error, String timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, nowUtc());
    }

    public static <T> ApiResponse<T> fail(String error) {
        return new ApiResponse<>(false, null, error, nowUtc());
    }

    private static String nowUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC).toString();
    }
}
