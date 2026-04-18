package com.ecorating.service;

import com.ecorating.client.NominatimGeocoder;
import com.ecorating.model.HazardObject;
import com.ecorating.model.HazardObject.HazardType;
import com.ecorating.util.MoscowOpenDataAddressParser;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Locale;
import java.util.Optional;
import java.util.StringJoiner;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * Промышленные предприятия (набор data.mos.ru, напр. идентификатор 2601): разбор {@code Cells} и координат.
 */
public final class MoscowIndustrialCatalogRow {

    public static final String SOURCE_DATASET_2601 = "data.mos.ru:2601";

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private MoscowIndustrialCatalogRow() {}

    public static Optional<HazardObject> toHazardObject(JsonNode row, NominatimGeocoder nominatimGeocoder) {
        JsonNode cells = row.path("Cells");
        if (cells.isMissingNode() || !cells.isObject()) {
            return Optional.empty();
        }

        long globalId = row.path("global_id").asLong(0L);
        if (globalId == 0L) {
            globalId = cells.path("global_id").asLong(0L);
        }
        if (globalId == 0L) {
            return Optional.empty();
        }

        String fullName = text(cells, "FullName");
        if (fullName.isBlank()) {
            fullName = text(cells, "ShortName");
        }
        if (fullName.isBlank()) {
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

        HazardType type = inferHazardType(cells);
        String description = buildDescription(cells);
        String descOrNull = description.isBlank() ? null : description;

        String name = truncate(fullName, 255);
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat));

        return Optional.of(new HazardObject(name, type, location, descOrNull, SOURCE_DATASET_2601, globalId));
    }

    private static String buildDescription(JsonNode cells) {
        StringJoiner j = new StringJoiner(" · ");
        String cat = text(cells, "Category");
        if (!cat.isBlank()) {
            j.add(cat);
        }
        String spec = text(cells, "Specialization");
        if (!spec.isBlank()) {
            j.add(spec);
        }
        String okved = text(cells, "OKVED");
        String okvedDesc = text(cells, "OKVED_Description");
        if (!okved.isBlank() || !okvedDesc.isBlank()) {
            j.add(okved.isBlank() ? okvedDesc : okved + " — " + okvedDesc);
        }
        String adm = text(cells, "AdmArea");
        String dist = text(cells, "District");
        if (!adm.isBlank() || !dist.isBlank()) {
            String area = adm.isBlank() ? dist : dist.isBlank() ? adm : adm + " / " + dist;
            j.add(area);
        }
        return j.toString();
    }

    private static HazardType inferHazardType(JsonNode cells) {
        String blob = (text(cells, "Category")
                        + " "
                        + text(cells, "Specialization")
                        + " "
                        + text(cells, "OKVED_Description")
                        + " "
                        + text(cells, "OKVED"))
                .toLowerCase(Locale.ROOT);
        if (containsAny(blob, "тэц", "теплоэлектро", "тепловая электростан", "энергетика", "электростан", "котельн")) {
            return HazardType.TPP;
        }
        if (containsAny(blob, "сжиган", "крематор", "инсинератор", "мусоросжиг")) {
            return HazardType.INCINERATOR;
        }
        if (containsAny(blob, "полигон", "свалк", "захоронен", "твердых отход")) {
            return HazardType.LANDFILL;
        }
        return HazardType.FACTORY;
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String n : needles) {
            if (haystack.contains(n)) {
                return true;
            }
        }
        return false;
    }

    private static String text(JsonNode cells, String key) {
        if (cells.has(key) && cells.get(key).isTextual()) {
            return cells.get(key).asText("").trim();
        }
        return "";
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
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
