package com.ecorating.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;

/**
 * Точка из набора data.mos.ru (поля в {@code Cells} зависят от конкретного датасета).
 */
record MoscowWifiPoint(double latitude, double longitude, String name, String address, String networkName) {

    static Optional<MoscowWifiPoint> fromRow(JsonNode row) {
        JsonNode cells = row.path("Cells");
        if (cells.isMissingNode() || !cells.isObject()) {
            return Optional.empty();
        }

        Double lat = extractLatitude(cells);
        Double lon = extractLongitude(cells);
        if (lat == null || lon == null) {
            return Optional.empty();
        }

        String name = firstNonBlank(
                cells,
                "Name",
                "ShortName",
                "Caption",
                "ObjectName",
                "FullName"
        );
        String address = firstNonBlank(
                cells,
                "Location",
                "Address",
                "FullAddress",
                "AdmArea",
                "District"
        );
        String network = firstNonBlank(
                cells,
                "WiFiName",
                "WiFi_network_name",
                "Wi_Fi_network_name",
                "SSID",
                "NetworkName",
                "WifiName"
        );

        if (name.isBlank()) {
            name = address.isBlank() ? "Городской Wi‑Fi" : address;
        }

        return Optional.of(new MoscowWifiPoint(lat, lon, name, address, network));
    }

    private static Double extractLatitude(JsonNode cells) {
        Double fromFields = parseCoord(cells, "Latitude_WGS84", "Latitude", "LAT", "Geo_Lat", "lat");
        if (fromFields != null) {
            return fromFields;
        }
        return fromGeoData(cells.path("geodata"), true);
    }

    private static Double extractLongitude(JsonNode cells) {
        Double fromFields = parseCoord(cells, "Longitude_WGS84", "Longitude", "LON", "Geo_Long", "lon");
        if (fromFields != null) {
            return fromFields;
        }
        return fromGeoData(cells.path("geodata"), false);
    }

    private static Double fromGeoData(JsonNode geo, boolean latitude) {
        if (geo.isMissingNode() || !geo.isObject()) {
            return null;
        }
        String type = geo.path("type").asText("");
        JsonNode coords = geo.path("coordinates");
        if (!"Point".equalsIgnoreCase(type) || !coords.isArray() || coords.size() < 2) {
            return null;
        }
        double c0 = coords.get(0).asDouble(Double.NaN);
        double c1 = coords.get(1).asDouble(Double.NaN);
        if (Double.isNaN(c0) || Double.isNaN(c1)) {
            return null;
        }
        // GeoJSON: [lon, lat]
        return latitude ? c1 : c0;
    }

    private static Double parseCoord(JsonNode cells, String... keys) {
        for (String key : keys) {
            if (cells.has(key)) {
                Double v = parseDouble(cells.get(key));
                if (v != null) {
                    return v;
                }
            }
        }
        return null;
    }

    private static Double parseDouble(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        if (node.isTextual()) {
            String s = node.asText("").trim().replace(',', '.').replace(" ", "");
            if (s.isEmpty()) {
                return null;
            }
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String firstNonBlank(JsonNode cells, String... keys) {
        for (String key : keys) {
            if (cells.has(key)) {
                JsonNode n = cells.get(key);
                if (n.isTextual()) {
                    String t = n.asText("").trim();
                    if (!t.isEmpty()) {
                        return t;
                    }
                } else if (n.isNumber()) {
                    return n.asText();
                }
            }
        }
        return "";
    }
}
