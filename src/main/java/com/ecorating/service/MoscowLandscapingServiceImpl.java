package com.ecorating.service;

import com.ecorating.dto.response.NearestLandscapingDto;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MoscowLandscapingServiceImpl implements MoscowLandscapingService {

    private static final Logger log = LoggerFactory.getLogger(MoscowLandscapingServiceImpl.class);

    private static final String SOURCE_LABEL = "data.mos.ru (благоустройство жилой застройки)";

    private static final int DEFAULT_LIMIT = 3;

    private static final double MIN_LAT = 55.35;
    private static final double MAX_LAT = 56.05;
    private static final double MIN_LON = 36.85;
    private static final double MAX_LON = 38.25;

    private final MoscowLandscapingRowsCache rowsCache;
    private final int landscapingDatasetId;
    private final int landscapingMinYear;
    private final int landscapingMaxRows;

    public MoscowLandscapingServiceImpl(
            MoscowLandscapingRowsCache rowsCache,
            @Value("${external.mos-data.landscaping-dataset-id:62961}") int landscapingDatasetId,
            @Value("${external.mos-data.landscaping-min-year:2024}") int landscapingMinYear,
            @Value("${external.mos-data.landscaping-max-rows:4000}") int landscapingMaxRows
    ) {
        this.rowsCache = rowsCache;
        this.landscapingDatasetId = landscapingDatasetId;
        this.landscapingMinYear = landscapingMinYear;
        this.landscapingMaxRows = landscapingMaxRows;
    }

    @Override
    public List<NearestLandscapingDto> findNearestLandscapingWorks(double lat, double lon, int limit) {
        int n = limit > 0 ? limit : DEFAULT_LIMIT;
        if (landscapingDatasetId <= 0) {
            return List.of();
        }
        if (!insideRoughMoscowBounds(lat, lon)) {
            return List.of();
        }

        try {
            List<MoscowLandscapingPoint> points = rowsCache.loadPoints(
                    landscapingDatasetId,
                    landscapingMinYear,
                    landscapingMaxRows
            );
            if (points.isEmpty()) {
                return List.of();
            }

            return points.stream()
                    .sorted(Comparator.comparingDouble(p -> haversineMeters(lat, lon, p.latitude(), p.longitude())))
                    .limit(n)
                    .map(p -> {
                        double distance = haversineMeters(lat, lon, p.latitude(), p.longitude());
                        return new NearestLandscapingDto(
                                p.workName().isBlank() ? "Работа по благоустройству" : p.workName(),
                                p.workEssence().isBlank() ? null : p.workEssence(),
                                p.detailedWork().isBlank() ? null : p.detailedWork(),
                                p.address().isBlank() ? null : p.address(),
                                p.admArea().isBlank() ? null : p.admArea(),
                                p.district().isBlank() ? null : p.district(),
                                p.yearOfWork() > 0 ? p.yearOfWork() : null,
                                p.workStatus().isBlank() ? null : p.workStatus(),
                                p.volumeSummary().isBlank() ? null : p.volumeSummary(),
                                p.latitude(),
                                p.longitude(),
                                distance,
                                SOURCE_LABEL
                        );
                    })
                    .toList();
        } catch (Exception ex) {
            log.warn("Moscow landscaping lookup failed: {}", ex.getMessage());
            return List.of();
        }
    }

    private static boolean insideRoughMoscowBounds(double lat, double lon) {
        return lat >= MIN_LAT && lat <= MAX_LAT && lon >= MIN_LON && lon <= MAX_LON;
    }

    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusM = 6_371_000.0;
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusM * c;
    }
}
