package com.ecorating.service;

public interface GreenZoneService {

    GreenZoneResult getGreenZones(double lat, double lon, int radiusMeters);
}
