package com.ecorating.client;

import com.ecorating.exception.ExternalApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Component
public class MoscowOpenDataClientImpl implements MoscowOpenDataClient {

    private static final Logger log = LoggerFactory.getLogger(MoscowOpenDataClientImpl.class);

    private static final int PAGE_SIZE = 500;
    private static final int MAX_ROWS = 100_000;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    /** Без завершающего слэша — для сборки URL с query в стиле OData ($top / $skip). */
    private final String mosDataBaseUrl;

    public MoscowOpenDataClientImpl(
            WebClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${external.mos-data.base-url:https://apidata.mos.ru}") String baseUrl,
            @Value("${external.mos-data.api-key:}") String apiKey
    ) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(120))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15_000);
        String normalized = baseUrl == null ? "https://apidata.mos.ru" : baseUrl.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        this.mosDataBaseUrl = normalized;
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
        this.webClient = builder
                .exchangeStrategies(strategies)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(this.mosDataBaseUrl)
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    /**
     * UriBuilder кодирует {@code $} в {@code %24} в именах параметров — apidata.mos.ru отвечает 400.
     * Параметр {@code $skip=0} этот API не принимает (400: «не может быть меньше 1») — первую страницу
     * запрашиваем без {@code $skip}, далее {@code $skip} = числу уже загруженных строк.
     */
    private URI rowsRequestUri(int datasetId, int pageSkip, String oDataFilter) {
        String keyQuery = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        sb.append(mosDataBaseUrl)
                .append("/v1/datasets/")
                .append(datasetId)
                .append("/rows?$top=")
                .append(PAGE_SIZE);
        if (oDataFilter != null && !oDataFilter.isBlank()) {
            sb.append("&$filter=").append(URLEncoder.encode(oDataFilter.trim(), StandardCharsets.UTF_8));
        }
        if (pageSkip > 0) {
            sb.append("&$skip=").append(pageSkip);
        }
        sb.append("&api_key=").append(keyQuery);
        return URI.create(sb.toString());
    }

    @Override
    public List<JsonNode> fetchAllRows(int datasetId) {
        return fetchRows(datasetId, null, MAX_ROWS);
    }

    @Override
    public List<JsonNode> fetchRows(int datasetId, String oDataFilter, int maxRows) {
        return fetchRows(datasetId, oDataFilter, maxRows, null);
    }

    @Override
    public List<JsonNode> fetchRows(
            int datasetId,
            String oDataFilter,
            int maxRows,
            Consumer<List<JsonNode>> afterEachPageAccumulated
    ) {
        if (apiKey.isEmpty()) {
            throw new ExternalApiException("MOS_DATA", "MOS_DATA API key is missing", null);
        }
        if (maxRows <= 0) {
            return List.of();
        }

        long startedAt = System.currentTimeMillis();
        List<JsonNode> out = new ArrayList<>();
        int skip = 0;
        int cap = Math.min(maxRows, MAX_ROWS);

        try {
            while (out.size() < cap) {
                final int pageSkip = skip;
                String raw = webClient.get()
                        .uri(rowsRequestUri(datasetId, pageSkip, oDataFilter))
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                if (raw == null || raw.isBlank()) {
                    break;
                }

                JsonNode root = objectMapper.readTree(raw);
                if (!root.isArray()) {
                    throw new ExternalApiException("MOS_DATA", "Unexpected response: expected JSON array", null);
                }

                if (root.isEmpty()) {
                    break;
                }

                for (JsonNode row : root) {
                    out.add(row);
                    if (out.size() >= cap) {
                        break;
                    }
                }

                if (afterEachPageAccumulated != null && !out.isEmpty()) {
                    afterEachPageAccumulated.accept(List.copyOf(out));
                }

                if (out.size() >= cap) {
                    break;
                }

                if (root.size() < PAGE_SIZE) {
                    break;
                }

                skip += PAGE_SIZE;
            }

            log.info(
                    "Moscow Open Data dataset {}: loaded {} rows in {} ms (filter={}, maxRows={})",
                    datasetId,
                    out.size(),
                    System.currentTimeMillis() - startedAt,
                    oDataFilter == null ? "none" : oDataFilter,
                    cap
            );
            return out;
        } catch (ExternalApiException ex) {
            log.warn("Moscow Open Data request failed: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.warn("Moscow Open Data request failed", ex);
            throw new ExternalApiException("MOS_DATA", "Moscow Open Data request failed", ex);
        }
    }
}
