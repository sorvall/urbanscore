package com.ecorating.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

public record NearbyObjectDto(
        String name,
        String type,
        double distanceMeters,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String description
) {
}
