package com.ecorating.scheduler;

import com.ecorating.client.AqicnClient;
import com.ecorating.repository.AirQualityReadingRepository;
import com.ecorating.service.AirQualityService;
import com.ecorating.service.GreenZoneService;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DataRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(DataRefreshScheduler.class);

    private final AirQualityReadingRepository airQualityReadingRepository;
    private final AirQualityService airQualityService;
    private final GreenZoneService greenZoneService;
    private final AqicnClient aqicnClient;

    public DataRefreshScheduler(
            AirQualityReadingRepository airQualityReadingRepository,
            AirQualityService airQualityService,
            GreenZoneService greenZoneService,
            AqicnClient aqicnClient
    ) {
        this.airQualityReadingRepository = airQualityReadingRepository;
        this.airQualityService = airQualityService;
        this.greenZoneService = greenZoneService;
        this.aqicnClient = aqicnClient;
    }

    @Scheduled(fixedRateString = "${scheduler.air-quality.interval-ms:3600000}")
    public void refreshAirQuality() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<AirQualityReadingRepository.LatLonProjection> coordinates = airQualityReadingRepository.findDistinctCoordinatesSince(since);

        int successCount = 0;
        int failureCount = 0;
        for (AirQualityReadingRepository.LatLonProjection coordinate : coordinates) {
            try {
                airQualityService.getAirQuality(coordinate.getLat(), coordinate.getLon());
                successCount++;
            } catch (Exception ex) {
                failureCount++;
                log.warn("Air quality refresh failed for {}, {}", coordinate.getLat(), coordinate.getLon(), ex);
            }
        }

        log.info("Air quality refresh finished: total={}, success={}, failed={}", coordinates.size(), successCount, failureCount);
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void refreshGreenZones() {
        List<AirQualityReadingRepository.LatLonProjection> coordinates = airQualityReadingRepository.findAllDistinctCoordinates();

        int successCount = 0;
        int failureCount = 0;
        for (AirQualityReadingRepository.LatLonProjection coordinate : coordinates) {
            try {
                greenZoneService.getGreenZones(coordinate.getLat(), coordinate.getLon(), 500);
                successCount++;
            } catch (Exception ex) {
                failureCount++;
                log.warn("Green zone refresh failed for {}, {}", coordinate.getLat(), coordinate.getLon(), ex);
            }
        }

        log.info("Green zones refresh finished: total={}, success={}, failed={}", coordinates.size(), successCount, failureCount);
    }

    @Scheduled(fixedRate = 60000)
    public void healthCheck() {
        try {
            boolean aqicn = aqicnClient.ping();
            log.info("AQICN health check: {}", aqicn ? "UP" : "DOWN");
        } catch (Exception ex) {
            log.warn("AQICN health check failed", ex);
        }
    }
}
