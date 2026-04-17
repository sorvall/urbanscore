package com.ecorating.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Статус прогрева кэша / снимков набора 62961 (для отладки UI). */
public record LandscapingWarmupStatusDto(
        @JsonProperty("dataset_enabled")
        boolean datasetEnabled,
        @JsonProperty("snapshot_store_enabled")
        boolean snapshotStoreEnabled,
        @JsonProperty("warmup_in_progress")
        boolean warmupInProgress,
        @JsonProperty("rows_snapshot_present")
        boolean rowsSnapshotPresent,
        @JsonProperty("points_snapshot_present")
        boolean pointsSnapshotPresent,
        @JsonProperty("summary")
        String summary
) {
}
