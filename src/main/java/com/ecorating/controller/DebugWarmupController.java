package com.ecorating.controller;

import com.ecorating.dto.response.ApiResponse;
import com.ecorating.dto.response.WarmupStatusResponse;
import com.ecorating.service.WarmupStatusService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Статус прогрева тяжёлых кэшей (благоустройство 62961). Выключается в проде через {@code app.debug.warmup-status-enabled=false}.
 */
@RestController
@RequestMapping("/api/v1/debug")
public class DebugWarmupController {

    private final boolean endpointEnabled;
    private final WarmupStatusService warmupStatusService;

    public DebugWarmupController(
            @Value("${app.debug.warmup-status-enabled:true}") boolean endpointEnabled,
            WarmupStatusService warmupStatusService
    ) {
        this.endpointEnabled = endpointEnabled;
        this.warmupStatusService = warmupStatusService;
    }

    @GetMapping("/warmup-status")
    public ResponseEntity<?> warmupStatus() {
        if (!endpointEnabled) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(ApiResponse.ok(warmupStatusService.getStatus()));
    }
}
