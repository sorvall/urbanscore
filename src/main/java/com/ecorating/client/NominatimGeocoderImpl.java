package com.ecorating.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Component
public class NominatimGeocoderImpl implements NominatimGeocoder {

    private static final Logger log = LoggerFactory.getLogger(NominatimGeocoderImpl.class);

    /** Политика Nominatim: не чаще ~1 запроса/с с одного IP. */
    private static final int MIN_INTERVAL_MS = 1100;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final Object paceLock = new Object();
    private long lastRequestAtMillis;

    /** Прокси на себя — чтобы @Cacheable сработал вне self-invocation. */
    private final NominatimGeocoderImpl self;

    public NominatimGeocoderImpl(
            WebClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${external.nominatim.base-url:https://nominatim.openstreetmap.org}") String baseUrl,
            @Value("${external.nominatim.enabled:true}") boolean enabled,
            @Lazy @Autowired NominatimGeocoderImpl self
    ) {
        this.self = self;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        String normalized = baseUrl == null ? "https://nominatim.openstreetmap.org" : baseUrl.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(30))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000);
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(512 * 1024))
                .build();
        this.webClient = builder
                .exchangeStrategies(strategies)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(normalized)
                .defaultHeader(HttpHeaders.USER_AGENT, "EcoRatingService/1.0 (urbanscore; nominatim geocoding)")
                .build();
    }

    @Override
    public Optional<GeoPoint> geocode(String address) {
        return Optional.ofNullable(self.geocodeCached(address));
    }

    @Cacheable(cacheNames = "nominatim-geocode", key = "#address", unless = "#result == null")
    public GeoPoint geocodeCached(String address) {
        if (!enabled || address == null || address.isBlank()) {
            return null;
        }
        String q = address.trim();
        if (q.length() < 8) {
            return null;
        }

        paceBeforeRequest();

        try {
            String raw = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("format", "json")
                            .queryParam("limit", 1)
                            .queryParam("countrycodes", "ru")
                            .queryParam("q", q)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (raw == null || raw.isBlank()) {
                return null;
            }

            JsonNode root = objectMapper.readTree(raw);
            if (!root.isArray() || root.isEmpty()) {
                return null;
            }

            JsonNode first = root.get(0);
            double lat = first.path("lat").asDouble(Double.NaN);
            double lon = first.path("lon").asDouble(Double.NaN);
            if (Double.isNaN(lat) || Double.isNaN(lon)) {
                return null;
            }

            return new GeoPoint(lat, lon);
        } catch (Exception ex) {
            log.warn("Nominatim geocoding failed for address snippet: {}", q.substring(0, Math.min(60, q.length())));
            return null;
        }
    }

    private void paceBeforeRequest() {
        synchronized (paceLock) {
            long now = System.currentTimeMillis();
            long wait = MIN_INTERVAL_MS - (now - lastRequestAtMillis);
            if (wait > 0) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            lastRequestAtMillis = System.currentTimeMillis();
        }
    }
}
