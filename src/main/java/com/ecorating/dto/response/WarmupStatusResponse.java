package com.ecorating.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WarmupStatusResponse(
        @JsonProperty("landscaping")
        LandscapingWarmupStatusDto landscaping,
        @JsonProperty("industry")
        IndustryWarmupStatusDto industry
) {
}
