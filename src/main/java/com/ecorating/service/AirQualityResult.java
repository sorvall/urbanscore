package com.ecorating.service;

import java.time.LocalDateTime;

public record AirQualityResult(
        double airQualityIndex,
        boolean stale,
        boolean hasData,
        LocalDateTime dataFreshnessTimestamp,
        String airQualitySource,
        AqicnDetails aqicnDetails
) {

    public static AirQualityResult empty() {
        return new AirQualityResult(0.0, true, false, LocalDateTime.now(), null, null);
    }
}
