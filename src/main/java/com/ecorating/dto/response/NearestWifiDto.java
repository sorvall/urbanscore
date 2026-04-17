package com.ecorating.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NearestWifiDto(
        @JsonProperty("name")
        String name,
        @JsonProperty("address")
        String address,
        @JsonProperty("network_name")
        String networkName,
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
