package com.ecorating.service;

import com.ecorating.dto.response.NearbyObjectDto;
import com.ecorating.model.HazardObject;
import com.ecorating.repository.HazardObjectRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HazardServiceImpl implements HazardService {

    private final HazardObjectRepository hazardObjectRepository;

    public HazardServiceImpl(HazardObjectRepository hazardObjectRepository) {
        this.hazardObjectRepository = hazardObjectRepository;
    }

    @Override
    public HazardResult getHazards(double lat, double lon, int radiusMeters) {
        List<HazardObject> hazards = hazardObjectRepository.findWithinRadius(lat, lon, radiusMeters);
        if (hazards.isEmpty()) {
            return HazardResult.empty();
        }

        double penalty = hazards.stream()
                .mapToDouble(hazard -> calculateDistanceWeightedPenalty(lat, lon, radiusMeters, hazard))
                .sum();

        double hazardIndex = Math.max(0.0, Math.min(100.0, 100.0 - penalty));

        List<NearbyObjectDto> nearest = hazards.stream()
                .map(hazard -> new NearbyObjectDto(
                        hazard.getName(),
                        hazard.getHazardType().name(),
                        haversineDistanceMeters(lat, lon, hazard.getLocation().getY(), hazard.getLocation().getX())
                ))
                .sorted(Comparator.comparingDouble(NearbyObjectDto::distanceMeters))
                .limit(10)
                .toList();

        LocalDateTime freshness = hazards.stream()
                .map(HazardObject::getUpdatedAt)
                .filter(v -> v != null)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        return new HazardResult(hazardIndex, nearest, true, freshness);
    }

    private double calculateDistanceWeightedPenalty(double lat, double lon, int radiusMeters, HazardObject hazard) {
        double basePenalty = switch (hazard.getHazardType()) {
            case FACTORY -> 20.0;
            case INCINERATOR -> 30.0;
            case LANDFILL -> 25.0;
            case TPP -> 18.0;
        };
        double distance = haversineDistanceMeters(lat, lon, hazard.getLocation().getY(), hazard.getLocation().getX());
        double distanceFactor = 1.0 - Math.min(1.0, distance / Math.max(1, radiusMeters));
        return basePenalty * distanceFactor;
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
