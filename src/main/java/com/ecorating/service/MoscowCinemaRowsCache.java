package com.ecorating.service;

import com.ecorating.client.MoscowOpenDataClient;
import com.ecorating.client.NominatimGeocoder;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class MoscowCinemaRowsCache {

    private static final Logger log = LoggerFactory.getLogger(MoscowCinemaRowsCache.class);

    private final MoscowOpenDataClient moscowOpenDataClient;
    private final NominatimGeocoder nominatimGeocoder;

    public MoscowCinemaRowsCache(
            MoscowOpenDataClient moscowOpenDataClient,
            NominatimGeocoder nominatimGeocoder
    ) {
        this.moscowOpenDataClient = moscowOpenDataClient;
        this.nominatimGeocoder = nominatimGeocoder;
    }

    @Cacheable(cacheNames = "mos-cinema-dataset-v3", key = "#datasetId")
    public List<MoscowCinemaPoint> loadPoints(int datasetId) {
        List<JsonNode> rows = moscowOpenDataClient.fetchAllRows(datasetId);
        List<MoscowCinemaPoint> points = rows.stream()
                .map(row -> MoscowCinemaPoint.fromRow(row, nominatimGeocoder))
                .flatMap(Optional::stream)
                .toList();
        log.info(
                "Moscow cinemas dataset {}: API rows={}, parsed points with coordinates={}",
                datasetId,
                rows.size(),
                points.size()
        );
        return points;
    }
}
