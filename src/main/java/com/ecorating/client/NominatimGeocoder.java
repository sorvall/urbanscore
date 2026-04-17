package com.ecorating.client;

import java.util.Optional;

/**
 * Прямое геокодирование адреса (используется, если в наборе data.mos.ru нет координат в строке).
 */
public interface NominatimGeocoder {

    record GeoPoint(double latitude, double longitude) {}

    /**
     * @param address полный адрес (например из ObjectAddress)
     */
    Optional<GeoPoint> geocode(String address);
}
