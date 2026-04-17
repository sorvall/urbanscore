package com.ecorating.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AqicnDataResponse(
        @JsonProperty("aqi")
        double aqi,
        @JsonProperty("dominant_pollutant")
        String dominantPollutant,
        @JsonProperty("pm25")
        double pm25,
        @JsonProperty("pm10")
        double pm10,
        @JsonProperty("temperature")
        Double temperature,
        @JsonProperty("humidity")
        Integer humidity,
        @JsonProperty("wind_speed")
        Double windSpeed,
        @JsonProperty("station_name")
        String stationName,
        @JsonProperty("last_update")
        String lastUpdate
) {
}
