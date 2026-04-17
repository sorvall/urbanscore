package com.ecorating.service;

import com.ecorating.dto.response.NearestWifiDto;
import java.util.List;

public interface MoscowWifiService {

    List<NearestWifiDto> findNearestCityWifi(double lat, double lon, int limit);
}
