package com.ecorating.service;

import com.ecorating.client.NominatimGeocoder;
import com.ecorating.util.MoscowOpenDataAddressParser;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import java.util.StringJoiner;

/** Работа по благоустройству (набор data.mos.ru, напр. 62961). */
public record MoscowLandscapingPoint(
        double latitude,
        double longitude,
        String address,
        String admArea,
        String district,
        String workName,
        String workEssence,
        String detailedWork,
        int yearOfWork,
        String workStatus,
        String volumeSummary
) {

    static Optional<MoscowLandscapingPoint> fromRow(JsonNode row, NominatimGeocoder nominatimGeocoder) {
        JsonNode cells = row.path("Cells");
        if (cells.isMissingNode() || !cells.isObject()) {
            return Optional.empty();
        }

        Double lat = extractLatitude(cells);
        Double lon = extractLongitude(cells);

        String address = MoscowOpenDataAddressParser.firstStreetAddressFromCells(cells);
        if ((lat == null || lon == null) && nominatimGeocoder != null && !address.isBlank()) {
            String q = MoscowOpenDataAddressParser.nominatimQueryFromOfficialAddress(address);
            Optional<NominatimGeocoder.GeoPoint> geo = nominatimGeocoder.geocode(q);
            if (geo.isPresent()) {
                lat = geo.get().latitude();
                lon = geo.get().longitude();
            }
        }

        if (lat == null || lon == null) {
            return Optional.empty();
        }

        String adm = textOrEmpty(cells, "AdmArea");
        String dist = textOrEmpty(cells, "District");
        String workName = textOrEmpty(cells, "WorkName");
        if (workName.isBlank()) {
            workName = textOrEmpty(cells, "DetailedWork");
        }
        String essence = textOrEmpty(cells, "WorkEssence");
        String detailed = textOrEmpty(cells, "DetailedWork");
        int year = cells.path("YearOfWork").asInt(0);
        String status = textOrEmpty(cells, "WorkStatus");
        String volume = volumeSummary(cells);

        return Optional.of(new MoscowLandscapingPoint(
                lat,
                lon,
                address,
                adm,
                dist,
                workName,
                essence,
                detailed,
                year,
                status,
                volume
        ));
    }

    private static String volumeSummary(JsonNode cells) {
        JsonNode arr = cells.path("WorkVolume");
        if (!arr.isArray() || arr.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("; ");
        for (JsonNode v : arr) {
            if (v != null && v.isObject()) {
                double vol = v.path("Volume").asDouble(Double.NaN);
                String unit = v.path("MeasuringUnit").asText("").trim();
                if (!Double.isNaN(vol)) {
                    joiner.add(unit.isEmpty() ? String.valueOf(vol) : vol + " " + unit);
                }
            }
        }
        return joiner.toString();
    }

    private static String textOrEmpty(JsonNode cells, String key) {
        if (cells.has(key) && cells.get(key).isTextual()) {
            return cells.get(key).asText("").trim();
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
