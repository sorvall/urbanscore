package com.ecorating.client;

import com.ecorating.exception.ExternalApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AqicnClientImpl implements AqicnClient {

    private static final Logger log = LoggerFactory.getLogger(AqicnClientImpl.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public AqicnClientImpl(
            WebClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${external.aqicn.base-url}") String baseUrl,
            @Value("${external.aqicn.api-key:}") String apiKey
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    public AirQualityPayload fetchAirQuality(double lat, double lon) {
        long startedAt = System.currentTimeMillis();
        try {
            if (apiKey.isEmpty()) {
                throw new ExternalApiException("AQICN", "AQICN token is missing", null);
            }

            String rawResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/feed/geo:" + String.format(Locale.ROOT, "%.6f;%.6f", lat, lon) + "/")
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (rawResponse == null || rawResponse.isBlank()) {
                throw new ExternalApiException("AQICN", "AQICN returned empty response", null);
            }

            JsonNode root = objectMapper.readTree(rawResponse);
            String status = root.path("status").asText("");
            if (!"ok".equalsIgnoreCase(status)) {
                String message = root.path("data").asText("unknown error");
                throw new ExternalApiException("AQICN", "AQICN response status is " + status + ": " + message, null);
            }

            JsonNode data = root.path("data");
            JsonNode iaqi = data.path("iaqi");

            double aqi = data.path("aqi").asDouble(0.0);
            String dominantPollutant = data.path("dominentpol").asText("");
            double pm25 = extractIaqiValue(iaqi, "pm25");
            double pm10 = extractIaqiValue(iaqi, "pm10");
            double no2 = extractIaqiValue(iaqi, "no2");
            Double temperature = extractNullableIaqiValue(iaqi, "t");
            Integer humidity = extractNullableIaqiIntValue(iaqi, "h");
            Double windSpeed = extractNullableIaqiValue(iaqi, "w");

            if (pm25 == 0.0 && pm10 == 0.0 && no2 == 0.0) {
                pm25 = aqi;
            }

            String source = data.path("city").path("name").asText("AQICN");
            LocalDateTime recordedAt = parseRecordedAt(data.path("time"));
            String recordedAtIso = toIsoString(recordedAt);

            log.info("AQICN request completed in {} ms", System.currentTimeMillis() - startedAt);
            return new AirQualityPayload(
                    aqi,
                    dominantPollutant.isBlank() ? null : dominantPollutant,
                    pm25,
                    pm10,
                    no2,
                    temperature,
                    humidity,
                    windSpeed,
                    source,
                    recordedAt,
                    recordedAtIso
            );
        } catch (ExternalApiException ex) {
            log.warn("AQICN request failed in {} ms: {}", System.currentTimeMillis() - startedAt, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.warn("AQICN request failed in {} ms", System.currentTimeMillis() - startedAt, ex);
            throw new ExternalApiException("AQICN", "AQICN request failed", ex);
        }
    }

    @Override
    public boolean ping() {
        long startedAt = System.currentTimeMillis();
        try {
            if (apiKey.isEmpty()) {
                return false;
            }
            webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/feed/geo:55.751244;37.618423/")
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("AQICN health ping completed in {} ms", System.currentTimeMillis() - startedAt);
            return true;
        } catch (Exception ex) {
            log.warn("AQICN health ping failed in {} ms", System.currentTimeMillis() - startedAt, ex);
            return false;
        }
    }

    private double extractIaqiValue(JsonNode iaqiNode, String metric) {
        JsonNode metricNode = iaqiNode.path(metric).path("v");
        if (metricNode.isNumber()) {
            return metricNode.asDouble();
        }
        return 0.0;
    }

    private Double extractNullableIaqiValue(JsonNode iaqiNode, String metric) {
        JsonNode metricNode = iaqiNode.path(metric).path("v");
        if (metricNode.isNumber()) {
            return metricNode.asDouble();
        }
        return null;
    }

    private Integer extractNullableIaqiIntValue(JsonNode iaqiNode, String metric) {
        JsonNode metricNode = iaqiNode.path(metric).path("v");
        if (metricNode.isNumber()) {
            return metricNode.asInt();
        }
        return null;
    }

    private LocalDateTime parseRecordedAt(JsonNode timeNode) {
        String iso = timeNode.path("iso").asText("");
        if (!iso.isBlank()) {
            try {
                return OffsetDateTime.parse(iso).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
            } catch (DateTimeParseException ignored) {
            }
        }

        String stationTime = timeNode.path("s").asText("");
        if (!stationTime.isBlank()) {
            try {
                return LocalDateTime.parse(stationTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (DateTimeParseException ignored) {
            }
        }

        return LocalDateTime.now();
    }

    private String toIsoString(LocalDateTime timestamp) {
        return timestamp.atOffset(ZoneOffset.UTC).toString();
    }
}
