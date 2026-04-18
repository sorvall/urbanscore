package com.ecorating.service;

import com.ecorating.client.OverpassClient;
import com.ecorating.client.OverpassClient.OverpassGreenZonePayload;
import com.ecorating.dto.response.NearbyObjectDto;
import com.ecorating.model.GreenZone;
import com.ecorating.repository.GreenZoneRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class GreenZoneServiceImpl implements GreenZoneService {

    private static final Logger log = LoggerFactory.getLogger(GreenZoneServiceImpl.class);
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final OverpassClient overpassClient;
    private final GreenZoneRepository greenZoneRepository;

    public GreenZoneServiceImpl(OverpassClient overpassClient, GreenZoneRepository greenZoneRepository) {
        this.overpassClient = overpassClient;
        this.greenZoneRepository = greenZoneRepository;
    }

    @Override
    @Cacheable(value = "green-zones", key = "T(java.lang.String).format(T(java.util.Locale).ROOT, '%.3f,%.3f,%d', #lat, #lon, #radiusMeters)")
    public GreenZoneResult getGreenZones(double lat, double lon, int radiusMeters) {
        try {
            List<OverpassGreenZonePayload> payloads = overpassClient.fetchGreenZones(lat, lon, radiusMeters);
            List<GreenZone> zones = payloads.stream()
                    .map(this::upsertZone)
                    .toList();
            return buildResult(zones, lat, lon, radiusMeters);
        } catch (Exception ex) {
            log.warn("Overpass call failed, falling back to DB: {}", ex.getMessage());
            List<GreenZone> fallbackZones = greenZoneRepository.findWithinRadius(lat, lon, radiusMeters);
            if (fallbackZones.isEmpty()) {
                return GreenZoneResult.empty();
            }
            return buildResult(fallbackZones, lat, lon, radiusMeters);
        }
    }

    private GreenZone upsertZone(OverpassGreenZonePayload payload) {
        GreenZone zone = greenZoneRepository.findByOsmId(payload.osmId())
                .orElseGet(() -> new GreenZone(payload.osmId(), payload.name(), payload.zoneType(), toPoint(payload.lat(), payload.lon()), payload.areaM2()));

        zone.setName(payload.name());
        zone.setZoneType(payload.zoneType());
        zone.setLocation(toPoint(payload.lat(), payload.lon()));
        zone.setAreaM2(payload.areaM2());

        return greenZoneRepository.save(zone);
    }

    private GreenZoneResult buildResult(List<GreenZone> zones, double lat, double lon, int radiusMeters) {
        double circleArea = Math.PI * radiusMeters * radiusMeters;
        double totalGreenArea = zones.stream()
                .mapToDouble(zone -> zone.getAreaM2() == null ? 0.0 : zone.getAreaM2())
                .sum();
        double ratio = circleArea <= 0 ? 0 : totalGreenArea / circleArea;
        double greenZoneIndex = Math.max(0.0, Math.min(100.0, ratio * 100.0));

        List<NearbyObjectDto> nearestParks = zones.stream()
                .map(zone -> new NearbyObjectDto(
                        zone.getName(),
                        zone.getZoneType().name(),
                        haversineDistanceMeters(lat, lon, zone.getLocation().getY(), zone.getLocation().getX()),
                        null
                ))
                .sorted(Comparator.comparingDouble(NearbyObjectDto::distanceMeters))
                .limit(5)
                .toList();

        LocalDateTime freshness = zones.stream()
                .map(GreenZone::getUpdatedAt)
                .filter(v -> v != null)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        return new GreenZoneResult(greenZoneIndex, nearestParks, true, freshness);
    }

    private Point toPoint(double lat, double lon) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat));
    }

    private double haversineDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}
