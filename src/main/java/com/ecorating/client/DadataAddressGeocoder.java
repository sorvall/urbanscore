package com.ecorating.client;

import com.ecorating.client.AddressGeocoder.AddressResolution;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class DadataAddressGeocoder implements AddressGeocoder {

    private static final Logger log = LoggerFactory.getLogger(DadataAddressGeocoder.class);
    private static final String GEOLOCATE_ADDRESS_PATH = "/suggestions/api/4_1/rs/geolocate/address";
    private static final String SUGGEST_ADDRESS_PATH = "/suggestions/api/4_1/rs/suggest/address";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String token;
    private final String secret;
    private final boolean enabled;
    private final long minIntervalMs;
    private final String division;

    private final ConcurrentHashMap<String, AddressResolution> geolocateCache = new ConcurrentHashMap<>();

    public DadataAddressGeocoder(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${external.dadata.base-url:https://suggestions.dadata.ru}") String baseUrl,
            @Value("${external.dadata.token:}") String token,
            @Value("${external.dadata.secret:}") String secret,
            @Value("${external.dadata.enabled:true}") boolean enabledFlag,
            @Value("${external.dadata.min-request-interval-ms:0}") long minIntervalMs,
            @Value("${external.dadata.address-division:administrative}") String addressDivision
    ) {
        this.webClient = webClientBuilder.baseUrl(trimTrailingSlash(baseUrl)).build();
        this.objectMapper = objectMapper;
        this.token = token;
        this.secret = secret;
        this.enabled = enabledFlag && StringUtils.hasText(token);
        this.minIntervalMs = Math.max(0, minIntervalMs);
        this.division = normalizeDivision(addressDivision);
    }

    private static String normalizeDivision(String raw) {
        if (raw == null || raw.isBlank()) {
            return "administrative";
        }
        String t = raw.trim().toLowerCase(Locale.ROOT);
        return "municipal".equals(t) ? "municipal" : "administrative";
    }

    private static String trimTrailingSlash(String u) {
        if (u == null || u.isEmpty()) {
            return "https://suggestions.dadata.ru";
        }
        return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }

    @Override
    public Optional<AddressResolution> resolveFromAddressQuery(String query) {
        if (!enabled || !StringUtils.hasText(query)) {
            return Optional.empty();
        }
        String q = query.trim();
        if (q.length() > 400) {
            q = q.substring(0, 400);
        }
        if (minIntervalMs > 0) {
            try {
                Thread.sleep(minIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("query", q);
            payload.put("count", 5);
            String bodyJson = objectMapper.writeValueAsString(payload);
            WebClient.RequestBodySpec req = webClient
                    .post()
                    .uri(SUGGEST_ADDRESS_PATH)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Token " + token);
            if (StringUtils.hasText(secret)) {
                req = req.header("X-Secret", secret);
            }
            String body = req.bodyValue(bodyJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(15));
            return parseFirstSuggestionBody(body);
        } catch (WebClientResponseException ex) {
            String errBody = ex.getResponseBodyAsString(StandardCharsets.UTF_8);
            log.warn(
                    "DaData suggest HTTP {}: {} — {}",
                    ex.getStatusCode().value(),
                    ex.getMessage(),
                    errBody != null && errBody.length() > 300 ? errBody.substring(0, 300) + "…" : errBody);
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("DaData suggest failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<AddressResolution> resolveFromCoordinates(double latitude, double longitude) {
        if (!enabled || !isValidLatLon(latitude, longitude)) {
            return Optional.empty();
        }
        String key = geolocateCacheKey(latitude, longitude);
        AddressResolution cached = geolocateCache.get(key);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<AddressResolution> result = resolveRemoteGeolocate(latitude, longitude);
        result.ifPresent(r -> geolocateCache.put(key, r));
        return result;
    }

    private static boolean isValidLatLon(double lat, double lon) {
        return lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0;
    }

    private static String geolocateCacheKey(double lat, double lon) {
        return String.format(Locale.ROOT, "%.6f\0%.6f\0v1", lat, lon);
    }

    private Optional<AddressResolution> resolveRemoteGeolocate(double latitude, double longitude) {
        if (minIntervalMs > 0) {
            try {
                Thread.sleep(minIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("lat", latitude);
            payload.put("lon", longitude);
            payload.put("count", 1);
            payload.put("radius_meters", 100);
            payload.put("division", division);
            String bodyJson = objectMapper.writeValueAsString(payload);
            WebClient.RequestBodySpec req = webClient
                    .post()
                    .uri(GEOLOCATE_ADDRESS_PATH)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Token " + token);
            if (StringUtils.hasText(secret)) {
                req = req.header("X-Secret", secret);
            }
            String body = req.bodyValue(bodyJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(15));
            return parseFirstSuggestionBody(body);
        } catch (WebClientResponseException ex) {
            String errBody = ex.getResponseBodyAsString(StandardCharsets.UTF_8);
            log.warn(
                    "DaData geolocate HTTP {}: {} — {}",
                    ex.getStatusCode().value(),
                    ex.getMessage(),
                    errBody != null && errBody.length() > 300 ? errBody.substring(0, 300) + "…" : errBody);
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("DaData geolocate failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<AddressResolution> parseFirstSuggestionBody(String body) {
        try {
            if (body == null || body.isBlank()) {
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode suggestions = root.path("suggestions");
            if (!suggestions.isArray() || suggestions.isEmpty()) {
                return Optional.empty();
            }
            JsonNode first = suggestions.get(0);
            JsonNode data = first.path("data");
            double lat = parseCoord(data.get("geo_lat"));
            double lon = parseCoord(data.get("geo_lon"));
            if (Double.isNaN(lat) || Double.isNaN(lon)) {
                return Optional.empty();
            }
            String value = textOrNull(first, "value");
            String unrestricted = textOrNull(first, "unrestricted_value");
            return Optional.of(
                    new AddressResolution(
                            lat,
                            lon,
                            value,
                            unrestricted,
                            textOrNull(data, "city_area"),
                            textOrNull(data, "city_district"),
                            textOrNull(data, "city_district_with_type"),
                            textOrNull(data, "area"),
                            textOrNull(data, "area_with_type"),
                            textOrNull(data, "region"),
                            textOrNull(data, "region_with_type")));
        } catch (Exception ex) {
            log.warn("DaData parse suggestions failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private static String textOrNull(JsonNode parent, String field) {
        if (parent == null || parent.isMissingNode()) {
            return null;
        }
        JsonNode n = parent.get(field);
        if (n == null || n.isNull()) {
            return null;
        }
        String s = n.asText("").trim();
        return s.isEmpty() ? null : s;
    }

    private static double parseCoord(JsonNode node) {
        if (node == null || node.isNull()) {
            return Double.NaN;
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        if (node.isTextual()) {
            String s = node.asText("").trim().replace(',', '.');
            if (s.isEmpty()) {
                return Double.NaN;
            }
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }
        return Double.NaN;
    }
}
