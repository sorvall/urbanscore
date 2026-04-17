package com.ecorating.client;

import java.time.LocalDateTime;

public interface AqicnClient {

    AirQualityPayload fetchAirQuality(double lat, double lon);

    boolean ping();

    record AirQualityPayload(
            double aqi,
            String dominantPollutant,
            double pm25,
            double pm10,
            double no2,
            Double temperature,
            Integer humidity,
            Double windSpeed,
            String source,
            LocalDateTime recordedAt,
            String recordedAtIso
    ) {
    }
}
