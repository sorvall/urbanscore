package com.ecorating.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReportResponse(String address, String html, String deepseekRequestDebug) {}
