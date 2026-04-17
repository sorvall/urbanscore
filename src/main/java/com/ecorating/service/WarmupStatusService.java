package com.ecorating.service;

import com.ecorating.dto.response.LandscapingWarmupStatusDto;
import com.ecorating.dto.response.WarmupStatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WarmupStatusService {

    private final MoscowLandscapingRowsCache moscowLandscapingRowsCache;
    private final MoscowLandscapingSnapshotStore snapshotStore;
    private final int landscapingDatasetId;
    private final int landscapingMinYear;
    private final int landscapingMaxRows;
    private final boolean snapshotEnabled;

    public WarmupStatusService(
            MoscowLandscapingRowsCache moscowLandscapingRowsCache,
            MoscowLandscapingSnapshotStore snapshotStore,
            @Value("${external.mos-data.landscaping-dataset-id:62961}") int landscapingDatasetId,
            @Value("${external.mos-data.landscaping-min-year:2024}") int landscapingMinYear,
            @Value("${external.mos-data.landscaping-max-rows:4000}") int landscapingMaxRows,
            @Value("${external.mos-data.landscaping-snapshot-enabled:true}") boolean snapshotEnabled
    ) {
        this.moscowLandscapingRowsCache = moscowLandscapingRowsCache;
        this.snapshotStore = snapshotStore;
        this.landscapingDatasetId = landscapingDatasetId;
        this.landscapingMinYear = landscapingMinYear;
        this.landscapingMaxRows = landscapingMaxRows;
        this.snapshotEnabled = snapshotEnabled;
    }

    public WarmupStatusResponse getStatus() {
        boolean ds = landscapingDatasetId > 0;
        boolean rows = ds && snapshotStore.rowsSnapshotExists(landscapingDatasetId, landscapingMinYear, landscapingMaxRows);
        boolean points = ds && snapshotStore.pointsSnapshotExists(landscapingDatasetId, landscapingMinYear, landscapingMaxRows);
        boolean warming = moscowLandscapingRowsCache.isLandscapingWarmupInProgress();

        String summary;
        if (!ds) {
            summary = "Набор благоустройства отключён (dataset id ≤ 0).";
        } else if (warming) {
            summary = "Идёт прогрев: скачивание data.mos.ru и/или геокодинг Nominatim.";
        } else if (points) {
            summary = "Прогрев завершён: есть снимок точек с координатами.";
        } else if (rows) {
            summary = "Есть снимок сырых строк; при следующем полном прогоне догеокодируются в точки.";
        } else {
            summary = "Снимков ещё нет — первый запрос /eco по Москве будет долгим.";
        }

        LandscapingWarmupStatusDto landscaping = new LandscapingWarmupStatusDto(
                ds,
                snapshotEnabled,
                warming,
                rows,
                points,
                summary
        );
        return new WarmupStatusResponse(landscaping);
    }
}
