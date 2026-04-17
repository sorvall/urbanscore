package com.ecorating.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record EcoReportResponse(
        double airQualityIndex,
        double greenZoneIndex,
        double hazardIndex,
        double overallEcoScore,
        List<NearbyObjectDto> nearestHazards,
        List<NearbyObjectDto> nearestParks,
        String dataFreshnessTimestamp,
        String airQualitySource,
        AqicnDataResponse aqicnData,
        @JsonProperty("nearest_city_wifi")
        List<NearestWifiDto> nearestCityWifi,
        @JsonProperty("nearest_cinemas")
        List<NearestCinemaDto> nearestCinemas,
        @JsonProperty("nearest_landscaping_works")
        List<NearestLandscapingDto> nearestLandscapingWorks
) {
}
