package com.ecorating.repository;

import com.ecorating.model.HazardObject;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HazardObjectRepository extends JpaRepository<HazardObject, Long> {

    void deleteAllBySource(String source);

    @Query(value = """
            SELECT *
            FROM hazard_objects h
            WHERE ST_DWithin(
                h.location,
                ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                :radiusMeters
            )
            """, nativeQuery = true)
    List<HazardObject> findWithinRadius(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusMeters") double radiusMeters
    );
}
