package com.ecorating.service;

import com.ecorating.client.NominatimGeocoder;
import com.ecorating.util.MoscowOpenDataAddressParser;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;

/** Кинотеатр из набора data.mos.ru (например идентификатор 495). */
record MoscowCinemaPoint(
        double latitude,
        double longitude,
        String commonName,
        String fullName,
        String address,
        String category,
        String phone
) {

    static Optional<MoscowCinemaPoint> fromRow(JsonNode row, NominatimGeocoder nominatimGeocoder) {
        JsonNode cells = row.path("Cells");
        if (cells.isMissingNode() || !cells.isObject()) {
            return Optional.empty();
        }

        Double lat = extractLatitude(cells);
        Double lon = extractLongitude(cells);

        String address = MoscowOpenDataAddressParser.firstStreetAddressFromCells(cells);
        if ((lat == null || lon == null) && nominatimGeocoder != null && !address.isBlank()) {
            String geocodeQuery = MoscowOpenDataAddressParser.nominatimQueryFromOfficialAddress(address);
            Optional<NominatimGeocoder.GeoPoint> geo = nominatimGeocoder.geocode(geocodeQuery);
            if (geo.isPresent()) {
                lat = geo.get().latitude();
                lon = geo.get().longitude();
            }
        }

        if (lat == null || lon == null) {
            return Optional.empty();
        }

        String common = textOrEmpty(cells, "CommonName", "ShortName");
        String full = textOrEmpty(cells, "FullName");
        if (common.isBlank()) {
            common = full.isBlank() ? "Кинотеатр" : full;
        }

        String category = textOrEmpty(cells, "Category");
        String phone = extractPublicOrChiefPhone(cells);

        return Optional.of(new MoscowCinemaPoint(lat, lon, common, full, address, category, phone));
    }

    private static String extractPublicOrChiefPhone(JsonNode cells) {
        JsonNode pub = cells.path("PublicPhone");
        if (pub.isArray()) {
            for (JsonNode n : pub) {
                if (n != null && n.isObject()) {
                    String p = n.path("PublicPhone").asText("").trim();
                    if (!p.isEmpty()) {
                        return p;
                    }
                }
            }
        }
        JsonNode orgs = cells.path("OrgInfo");
        if (orgs.isArray()) {
            for (JsonNode org : orgs) {
                JsonNode phones = org.path("ChiefPhone");
                if (phones.isArray() && phones.size() > 0) {
                    String p = phones.get(0).path("ChiefPhone").asText("").trim();
                    if (!p.isEmpty()) {
                        return p;
                    }
                }
            }
        }
        return "";
    }

    private static String textOrEmpty(JsonNode cells, String... keys) {
        for (String key : keys) {
            if (cells.has(key) && cells.get(key).isTextual()) {
                String t = cells.get(key).asText("").trim();
                if (!t.isEmpty()) {
                    return t;
                }
            }
        }
        return "";
    }

    private static JsonNode geoNode(JsonNode cells) {
        if (cells.has("geoData")) {
            return cells.get("geoData");
        }
        if (cells.has("geodata")) {
            return cells.get("geodata");
        }
        return cells.path("geodata");
    }

    private static Double extractLatitude(JsonNode cells) {
        Double fromFields = parseCoord(cells, "Latitude_WGS84", "Latitude", "LAT");
        if (fromFields != null) {
            return fromFields;
        }
        return fromGeoData(geoNode(cells), true);
    }

    private static Double extractLongitude(JsonNode cells) {
        Double fromFields = parseCoord(cells, "Longitude_WGS84", "Longitude", "LON");
        if (fromFields != null) {
            return fromFields;
        }
        return fromGeoData(geoNode(cells), false);
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
}
