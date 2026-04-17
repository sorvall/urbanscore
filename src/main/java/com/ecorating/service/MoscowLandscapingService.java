package com.ecorating.service;

import com.ecorating.dto.response.NearestLandscapingDto;
import java.util.List;

public interface MoscowLandscapingService {

    List<NearestLandscapingDto> findNearestLandscapingWorks(double lat, double lon, int limit);
}
