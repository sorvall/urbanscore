package com.ecorating.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NearestCinemaDto(
        @JsonProperty("name")
        String name,
        @JsonProperty("full_name")
        String fullName,
        @JsonProperty("address")
        String address,
        @JsonProperty("category")
        String category,
        @JsonProperty("phone")
        String phone,
        @JsonProperty("latitude")
        double latitude,
        @JsonProperty("longitude")
        double longitude,
        @JsonProperty("distance_meters")
        double distanceMeters,
        @JsonProperty("source")
        String source
) {
}
