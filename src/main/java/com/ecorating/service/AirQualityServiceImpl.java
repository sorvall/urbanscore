package com.ecorating.service;

import com.ecorating.client.AqicnClient;
import com.ecorating.client.AqicnClient.AirQualityPayload;
import com.ecorating.model.AirQualityReading;
import com.ecorating.repository.AirQualityReadingRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class AirQualityServiceImpl implements AirQualityService {

    private static final Logger log = LoggerFactory.getLogger(AirQualityServiceImpl.class);
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final AqicnClient aqicnClient;
    private final AirQualityReadingRepository airQualityReadingRepository;

    public AirQualityServiceImpl(
            AqicnClient aqicnClient,
            AirQualityReadingRepository airQualityReadingRepository
    ) {
        this.aqicnClient = aqicnClient;
        this.airQualityReadingRepository = airQualityReadingRepository;
    }

    @Override
    @Cacheable(value = "air-quality", key = "T(java.lang.String).format(T(java.util.Locale).ROOT, '%.3f,%.3f', #lat, #lon)")
    public AirQualityResult getAirQuality(double lat, double lon) {
        try {
            AirQualityPayload payload = aqicnClient.fetchAirQuality(lat, lon);

            int aqiScore = calculateAqiScore(payload.pm25(), payload.pm10(), payload.no2());

            AirQualityReading reading = new AirQualityReading(
                    lat,
                    lon,
                    toPoint(lat, lon),
                    payload.pm25(),
                    payload.pm10(),
                    payload.no2(),
                    aqiScore,
                    payload.source(),
                    payload.recordedAt() == null ? LocalDateTime.now() : payload.recordedAt()
            );
            airQualityReadingRepository.save(reading);

            AqicnDetails aqicnDetails = new AqicnDetails(
                    payload.aqi(),
                    payload.dominantPollutant(),
                    payload.pm25(),
                    payload.pm10(),
                    payload.temperature(),
                    payload.humidity(),
                    payload.windSpeed(),
                    payload.source(),
                    payload.recordedAtIso()
            );

            return new AirQualityResult(
                    aqiScore,
                    false,
                    true,
                    reading.getRecordedAt(),
                    "AQICN",
                    aqicnDetails
            );
        } catch (Exception ex) {
            log.warn("Air quality API failed, falling back to DB: {}", ex.getMessage());
            Optional<AirQualityReading> fallback = airQualityReadingRepository.findMostRecentWithinRadius(lat, lon, 5000.0);
            if (fallback.isPresent()) {
                AirQualityReading cached = fallback.get();
                return new AirQualityResult(
                        cached.getAqiScore(),
                        true,
                        true,
                        cached.getRecordedAt(),
                        "Кэш (БД)",
                        null
                );
            }
            return AirQualityResult.empty();
        }
    }

    private Point toPoint(double lat, double lon) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat));
    }

    private int calculateAqiScore(double pm25, double pm10, double no2) {
        // Conservative normalization that caps each pollutant contribution at 100.
        double pm25Norm = Math.min(100.0, (pm25 / 25.0) * 100.0);
        double pm10Norm = Math.min(100.0, (pm10 / 50.0) * 100.0);
        double no2Norm = Math.min(100.0, (no2 / 40.0) * 100.0);

        double pollutionRisk = (pm25Norm * 0.5) + (pm10Norm * 0.3) + (no2Norm * 0.2);
        double cleanAirScore = 100.0 - pollutionRisk;
        return (int) Math.max(0, Math.min(100, Math.round(cleanAirScore)));
    }
}
