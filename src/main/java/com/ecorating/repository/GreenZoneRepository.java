package com.ecorating.repository;

import com.ecorating.model.GreenZone;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GreenZoneRepository extends JpaRepository<GreenZone, Long> {

    Optional<GreenZone> findByOsmId(Long osmId);

    @Query(value = """
            SELECT *
            FROM green_zones g
            WHERE ST_DWithin(
                g.location,
                ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                :radiusMeters
            )
            """, nativeQuery = true)
    List<GreenZone> findWithinRadius(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusMeters") double radiusMeters
    );
}
