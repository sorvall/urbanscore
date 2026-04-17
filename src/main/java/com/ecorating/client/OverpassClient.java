package com.ecorating.client;

import com.ecorating.model.GreenZone;
import java.util.List;

public interface OverpassClient {

    List<OverpassGreenZonePayload> fetchGreenZones(double lat, double lon, int radiusMeters);

    record OverpassGreenZonePayload(
            long osmId,
            String name,
            GreenZone.ZoneType zoneType,
            double lat,
            double lon,
            double areaM2
    ) {
    }
}
