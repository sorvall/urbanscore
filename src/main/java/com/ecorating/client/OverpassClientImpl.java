package com.ecorating.client;

import com.ecorating.exception.ExternalApiException;
import com.ecorating.model.GreenZone;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class OverpassClientImpl implements OverpassClient {

    private static final Logger log = LoggerFactory.getLogger(OverpassClientImpl.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public OverpassClientImpl(
            WebClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${external.overpass.base-url}") String baseUrl
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public List<OverpassGreenZonePayload> fetchGreenZones(double lat, double lon, int radiusMeters) {
        long startedAt = System.currentTimeMillis();
        String query = buildQuery(lat, lon, radiusMeters);

        try {
            String rawResponse = webClient.post()
                    .contentType(MediaType.TEXT_PLAIN)
                    .bodyValue(query)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (rawResponse == null || rawResponse.isBlank()) {
                throw new ExternalApiException("Overpass", "Overpass returned empty response", null);
            }

            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode elements = root.path("elements");
            if (!elements.isArray()) {
                throw new ExternalApiException("Overpass", "Overpass response has invalid format", null);
            }

            Map<Long, OverpassGreenZonePayload> uniquePayloads = new LinkedHashMap<>();
            for (JsonNode element : elements) {
                long osmId = element.path("id").asLong(-1L);
                if (osmId <= 0) {
                    continue;
                }

                JsonNode tags = element.path("tags");
                GreenZone.ZoneType zoneType = resolveZoneType(tags);
                if (zoneType == null) {
                    continue;
                }

                Coordinates coords = resolveCoordinates(element);
                if (coords == null) {
                    continue;
                }

                String name = resolveName(tags, zoneType, osmId);
                double area = resolveArea(tags, zoneType);

                uniquePayloads.put(osmId, new OverpassGreenZonePayload(
                        osmId,
                        name,
                        zoneType,
                        coords.lat(),
                        coords.lon(),
                        area
                ));
            }

            List<OverpassGreenZonePayload> payloads = new ArrayList<>(uniquePayloads.values());
            log.info("Overpass request completed in {} ms, fetched {} objects", System.currentTimeMillis() - startedAt, payloads.size());
            return payloads;
        } catch (ExternalApiException ex) {
            log.warn("Overpass request failed in {} ms: {}", System.currentTimeMillis() - startedAt, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.warn("Overpass request failed in {} ms", System.currentTimeMillis() - startedAt, ex);
            throw new ExternalApiException("Overpass", "Overpass request failed", ex);
        }
    }

    private String buildQuery(double lat, double lon, int radiusMeters) {
        return "[out:json];(" +
                "way[\"leisure\"~\"park|garden\"](around:" + radiusMeters + "," + lat + "," + lon + ");" +
                "relation[\"leisure\"~\"park|garden\"](around:" + radiusMeters + "," + lat + "," + lon + ");" +
                "way[\"landuse\"=\"forest\"](around:" + radiusMeters + "," + lat + "," + lon + ");" +
                "relation[\"landuse\"=\"forest\"](around:" + radiusMeters + "," + lat + "," + lon + ");" +
                "way[\"natural\"=\"wood\"](around:" + radiusMeters + "," + lat + "," + lon + ");" +
                "relation[\"natural\"=\"wood\"](around:" + radiusMeters + "," + lat + "," + lon + ");" +
                ");out center tags;";
    }

    private GreenZone.ZoneType resolveZoneType(JsonNode tags) {
        String leisure = tags.path("leisure").asText("");
        String landuse = tags.path("landuse").asText("");
        String natural = tags.path("natural").asText("");

        if ("forest".equalsIgnoreCase(landuse) || "wood".equalsIgnoreCase(natural)) {
            return GreenZone.ZoneType.FOREST;
        }
        if ("garden".equalsIgnoreCase(leisure)) {
            return GreenZone.ZoneType.SQUARE;
        }
        if ("park".equalsIgnoreCase(leisure)) {
            return GreenZone.ZoneType.PARK;
        }
        return null;
    }

    private Coordinates resolveCoordinates(JsonNode element) {
        JsonNode center = element.path("center");
        if (center.path("lat").isNumber() && center.path("lon").isNumber()) {
            return new Coordinates(center.path("lat").asDouble(), center.path("lon").asDouble());
        }

        if (element.path("lat").isNumber() && element.path("lon").isNumber()) {
            return new Coordinates(element.path("lat").asDouble(), element.path("lon").asDouble());
        }

        return null;
    }

    private String resolveName(JsonNode tags, GreenZone.ZoneType zoneType, long osmId) {
        String name = tags.path("name").asText("").trim();
        if (!name.isEmpty()) {
            return name;
        }
        return zoneType.name() + "_OSM_" + osmId;
    }

    private double resolveArea(JsonNode tags, GreenZone.ZoneType zoneType) {
        JsonNode areaTag = tags.path("area");
        if (areaTag.isNumber()) {
            return Math.max(0.0, areaTag.asDouble());
        }

        return switch (zoneType) {
            case PARK -> 20000.0;
            case FOREST -> 50000.0;
            case SQUARE -> 8000.0;
        };
    }

    private record Coordinates(double lat, double lon) {
    }
}
