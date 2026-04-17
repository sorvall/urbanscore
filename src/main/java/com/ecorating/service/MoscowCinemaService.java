package com.ecorating.service;

import com.ecorating.dto.response.NearestCinemaDto;
import java.util.List;

public interface MoscowCinemaService {

    List<NearestCinemaDto> findNearestCinemas(double lat, double lon, int limit);
}
