package com.ecorating.controller;

import com.ecorating.dto.response.ApiResponse;
import com.ecorating.dto.response.AqicnDataResponse;
import com.ecorating.dto.response.EcoReportResponse;
import com.ecorating.dto.response.NearestCinemaDto;
import com.ecorating.dto.response.NearestLandscapingDto;
import com.ecorating.dto.response.NearestWifiDto;
import com.ecorating.service.AirQualityResult;
import com.ecorating.service.AirQualityService;
import com.ecorating.service.EcoScoreCalculator;
import com.ecorating.service.GreenZoneResult;
import com.ecorating.service.GreenZoneService;
import com.ecorating.service.HazardResult;
import com.ecorating.service.HazardService;
import com.ecorating.service.MoscowCinemaService;
import com.ecorating.service.MoscowLandscapingService;
import com.ecorating.service.MoscowWifiService;
import java.util.List;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class EcoController {

    private static final Logger log = LoggerFactory.getLogger(EcoController.class);

    private final AirQualityService airQualityService;
    private final GreenZoneService greenZoneService;
    private final HazardService hazardService;
    private final EcoScoreCalculator ecoScoreCalculator;
    private final MoscowWifiService moscowWifiService;
    private final MoscowCinemaService moscowCinemaService;
    private final MoscowLandscapingService moscowLandscapingService;

    public EcoController(
            AirQualityService airQualityService,
            GreenZoneService greenZoneService,
            HazardService hazardService,
            EcoScoreCalculator ecoScoreCalculator,
            MoscowWifiService moscowWifiService,
            MoscowCinemaService moscowCinemaService,
            MoscowLandscapingService moscowLandscapingService
    ) {
        this.airQualityService = airQualityService;
        this.greenZoneService = greenZoneService;
        this.hazardService = hazardService;
        this.ecoScoreCalculator = ecoScoreCalculator;
        this.moscowWifiService = moscowWifiService;
        this.moscowCinemaService = moscowCinemaService;
        this.moscowLandscapingService = moscowLandscapingService;
    }

    @GetMapping("/eco")
    public ApiResponse<EcoReportResponse> getEcoReport(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double lat,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double lon
    ) {
        long startedAt = System.currentTimeMillis();

        AirQualityResult air = airQualityService.getAirQuality(lat, lon);
        GreenZoneResult green = greenZoneService.getGreenZones(lat, lon, 500);
        HazardResult hazard = hazardService.getHazards(lat, lon, 500);

        double overall = ecoScoreCalculator.calculate(
                air.airQualityIndex(),
                green.greenZoneIndex(),
                hazard.hazardIndex()
        );

        String freshness = maxTimestamp(
                air.dataFreshnessTimestamp().toString(),
                green.dataFreshnessTimestamp().toString(),
                hazard.dataFreshnessTimestamp().toString()
        );

        List<NearestWifiDto> nearestWifi = moscowWifiService.findNearestCityWifi(lat, lon, 3);
        List<NearestCinemaDto> nearestCinemas = moscowCinemaService.findNearestCinemas(lat, lon, 3);
        List<NearestLandscapingDto> nearestLandscaping = moscowLandscapingService.findNearestLandscapingWorks(lat, lon, 3);

        EcoReportResponse response = new EcoReportResponse(
                air.airQualityIndex(),
                green.greenZoneIndex(),
                hazard.hazardIndex(),
                overall,
                hazard.nearestHazards(),
                green.nearestParks(),
                freshness,
                air.airQualitySource(),
                mapAqicnData(air),
                nearestWifi,
                nearestCinemas,
                nearestLandscaping
        );

        log.info("GET /api/v1/eco completed in {} ms", System.currentTimeMillis() - startedAt);
        return ApiResponse.ok(response);
    }

    private String maxTimestamp(String a, String b, String c) {
        return java.util.stream.Stream.of(a, b, c)
                .max(String::compareTo)
                .orElse(a);
    }

    private AqicnDataResponse mapAqicnData(AirQualityResult air) {
        if (air.aqicnDetails() == null) {
            return null;
        }
        return new AqicnDataResponse(
                air.aqicnDetails().aqi(),
                air.aqicnDetails().dominantPollutant(),
                air.aqicnDetails().pm25(),
                air.aqicnDetails().pm10(),
                air.aqicnDetails().temperature(),
                air.aqicnDetails().humidity(),
                air.aqicnDetails().windSpeed(),
                air.aqicnDetails().stationName(),
                air.aqicnDetails().lastUpdate()
        );
    }

}
