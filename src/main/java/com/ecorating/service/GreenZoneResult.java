package com.ecorating.service;

import com.ecorating.dto.response.NearbyObjectDto;
import java.time.LocalDateTime;
import java.util.List;

public record GreenZoneResult(
        double greenZoneIndex,
        List<NearbyObjectDto> nearestParks,
        boolean hasData,
        LocalDateTime dataFreshnessTimestamp
) {

    public static GreenZoneResult empty() {
        return new GreenZoneResult(0.0, List.of(), false, LocalDateTime.now());
    }
}
