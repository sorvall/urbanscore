package com.ecorating.dto;

import jakarta.validation.constraints.NotBlank;

public record ReportRequest(@NotBlank String address) {}
