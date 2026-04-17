package com.ecorating.service;

import com.ecorating.dto.response.NearbyObjectDto;
import java.time.LocalDateTime;
import java.util.List;

public record HazardResult(
        double hazardIndex,
        List<NearbyObjectDto> nearestHazards,
        boolean hasData,
        LocalDateTime dataFreshnessTimestamp
) {

    public static HazardResult empty() {
        return new HazardResult(100.0, List.of(), false, LocalDateTime.now());
    }
}
