package com.ecorating.service;

import com.ecorating.client.MoscowOpenDataClient;
import com.ecorating.client.NominatimGeocoder;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class MoscowLandscapingRowsCache {

    private static final Logger log = LoggerFactory.getLogger(MoscowLandscapingRowsCache.class);

    private final MoscowOpenDataClient moscowOpenDataClient;
    private final NominatimGeocoder nominatimGeocoder;
    private final MoscowLandscapingSnapshotStore snapshotStore;

    /** Сериализация первого полного прогрева (скачивание + геокодинг). */
    private final Object loadLock = new Object();

    /** true, пока в памяти выполняется загрузка без готового снимка точек (для /debug/warmup-status). */
    private final AtomicBoolean landscapingWarmupInProgress = new AtomicBoolean(false);

    public MoscowLandscapingRowsCache(
            MoscowOpenDataClient moscowOpenDataClient,
            NominatimGeocoder nominatimGeocoder,
            MoscowLandscapingSnapshotStore snapshotStore
    ) {
        this.moscowOpenDataClient = moscowOpenDataClient;
        this.nominatimGeocoder = nominatimGeocoder;
        this.snapshotStore = snapshotStore;
    }

    public boolean isLandscapingWarmupInProgress() {
        return landscapingWarmupInProgress.get();
    }

    @Cacheable(cacheNames = "mos-landscaping-dataset-v3", key = "#datasetId + '-' + #minYear + '-' + #maxRows")
    public List<MoscowLandscapingPoint> loadPoints(int datasetId, int minYear, int maxRows) {
        synchronized (loadLock) {
            Optional<List<MoscowLandscapingPoint>> fromPoints = snapshotStore.loadPointsIfPresent(
                    datasetId,
                    minYear,
                    maxRows
            );
            if (fromPoints.isPresent()) {
                return fromPoints.get();
            }

            landscapingWarmupInProgress.set(true);
            try {
                String filter = "Cells/YearOfWork ge " + minYear;
                Optional<List<JsonNode>> rowsSnapshot = snapshotStore.loadRowsIfPresent(datasetId, minYear, maxRows);
                List<JsonNode> rows;
                if (rowsSnapshot.isPresent()) {
                    rows = rowsSnapshot.get();
                } else {
                    rows = moscowOpenDataClient.fetchRows(
                            datasetId,
                            filter,
                            maxRows,
                            partial -> snapshotStore.saveRows(partial, datasetId, minYear, maxRows)
                    );
                    if (rows.isEmpty()) {
                        log.warn("Moscow landscaping dataset {}: no rows from API", datasetId);
                        return List.of();
                    }
                }

                List<MoscowLandscapingPoint> points = rows.stream()
                        .map(row -> MoscowLandscapingPoint.fromRow(row, nominatimGeocoder))
                        .flatMap(Optional::stream)
                        .toList();

                snapshotStore.savePoints(points, datasetId, minYear, maxRows);

                log.info(
                        "Moscow landscaping dataset {} (YearOfWork>= {}): source rows={}, parsed points with coordinates={}",
                        datasetId,
                        minYear,
                        rows.size(),
                        points.size()
                );
                return points;
            } finally {
                landscapingWarmupInProgress.set(false);
            }
        }
    }
}
