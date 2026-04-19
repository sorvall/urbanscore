package com.ecorating.controller;

import com.ecorating.dto.ApiResponse;
import com.ecorating.dto.ReportRequest;
import com.ecorating.dto.ReportResponse;
import com.ecorating.service.DeepSeekService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class ReportController {

    private final DeepSeekService deepSeekService;

    public ReportController(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
    }

    @PostMapping("/report")
    public ApiResponse<ReportResponse> report(@Valid @RequestBody ReportRequest request) {
        String userMessage = "Адрес объекта: " + request.address().trim() + "\n\n" + request.prompt().trim();
        String html;
        try {
            html = deepSeekService.complete(userMessage);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        }
        return ApiResponse.ok(new ReportResponse(request.address().trim(), html));
    }
}
