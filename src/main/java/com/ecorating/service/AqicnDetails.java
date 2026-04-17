package com.ecorating.service;

public record AqicnDetails(
        double aqi,
        String dominantPollutant,
        double pm25,
        double pm10,
        Double temperature,
        Integer humidity,
        Double windSpeed,
        String stationName,
        String lastUpdate
) {
}
