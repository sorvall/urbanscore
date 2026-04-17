package com.ecorating.service;

import com.ecorating.client.MoscowOpenDataClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class MoscowWifiRowsCache {

    private final MoscowOpenDataClient moscowOpenDataClient;

    public MoscowWifiRowsCache(MoscowOpenDataClient moscowOpenDataClient) {
        this.moscowOpenDataClient = moscowOpenDataClient;
    }

    @Cacheable(cacheNames = "mos-wifi-dataset", key = "#datasetId")
    public List<MoscowWifiPoint> loadPoints(int datasetId) {
        List<JsonNode> rows = moscowOpenDataClient.fetchAllRows(datasetId);
        return rows.stream()
                .map(MoscowWifiPoint::fromRow)
                .flatMap(Optional::stream)
                .toList();
    }
}
