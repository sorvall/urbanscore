package com.ecorating.dto.response;

public record NearbyObjectDto(
        String name,
        String type,
        double distanceMeters
) {
}
