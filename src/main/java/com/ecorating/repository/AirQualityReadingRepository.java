package com.ecorating.repository;

import com.ecorating.model.AirQualityReading;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AirQualityReadingRepository extends JpaRepository<AirQualityReading, Long> {

    interface LatLonProjection {
        double getLat();

        double getLon();
    }

    @Query(value = """
            SELECT *
            FROM air_quality_readings a
            WHERE ST_DWithin(
                a.location,
                ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                :radiusMeters
            )
            ORDER BY a.recorded_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<AirQualityReading> findMostRecentWithinRadius(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusMeters") double radiusMeters
    );

    @Query(value = """
            SELECT DISTINCT a.lat AS lat, a.lon AS lon
            FROM air_quality_readings a
            WHERE a.recorded_at >= :since
            """, nativeQuery = true)
    List<LatLonProjection> findDistinctCoordinatesSince(@Param("since") LocalDateTime since);

    @Query(value = """
            SELECT DISTINCT a.lat AS lat, a.lon AS lon
            FROM air_quality_readings a
            """, nativeQuery = true)
    List<LatLonProjection> findAllDistinctCoordinates();
}
