package com.ecorating.service;

import com.ecorating.dto.response.NearestCinemaDto;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MoscowCinemaServiceImpl implements MoscowCinemaService {

    private static final Logger log = LoggerFactory.getLogger(MoscowCinemaServiceImpl.class);

    private static final String SOURCE_LABEL = "data.mos.ru (справочник кинотеатров)";

    private static final int DEFAULT_LIMIT = 3;

    private static final double MIN_LAT = 55.35;
    private static final double MAX_LAT = 56.05;
    private static final double MIN_LON = 36.85;
    private static final double MAX_LON = 38.25;

    private final MoscowCinemaRowsCache rowsCache;
    private final int cinemaDatasetId;

    public MoscowCinemaServiceImpl(
            MoscowCinemaRowsCache rowsCache,
            @Value("${external.mos-data.cinema-dataset-id:495}") int cinemaDatasetId
    ) {
        this.rowsCache = rowsCache;
        this.cinemaDatasetId = cinemaDatasetId;
    }

    @Override
    public List<NearestCinemaDto> findNearestCinemas(double lat, double lon, int limit) {
        int n = limit > 0 ? limit : DEFAULT_LIMIT;
        if (cinemaDatasetId <= 0) {
            return List.of();
        }
        if (!insideRoughMoscowBounds(lat, lon)) {
            return List.of();
        }

        try {
            List<MoscowCinemaPoint> points = rowsCache.loadPoints(cinemaDatasetId);
            if (points.isEmpty()) {
                return List.of();
            }

            return points.stream()
                    .sorted(Comparator.comparingDouble(p -> haversineMeters(lat, lon, p.latitude(), p.longitude())))
                    .limit(n)
                    .map(p -> {
                        double distance = haversineMeters(lat, lon, p.latitude(), p.longitude());
                        return new NearestCinemaDto(
                                p.commonName(),
                                p.fullName().isBlank() ? null : p.fullName(),
                                p.address().isBlank() ? null : p.address(),
                                p.category().isBlank() ? null : p.category(),
                                p.phone().isBlank() ? null : p.phone(),
                                p.latitude(),
                                p.longitude(),
                                distance,
                                SOURCE_LABEL
                        );
                    })
                    .toList();
        } catch (Exception ex) {
            log.warn("Moscow cinemas lookup failed: {}", ex.getMessage());
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
