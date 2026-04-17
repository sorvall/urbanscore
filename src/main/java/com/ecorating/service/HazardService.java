package com.ecorating.service;

public interface HazardService {

    HazardResult getHazards(double lat, double lon, int radiusMeters);
}
