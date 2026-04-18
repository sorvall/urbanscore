package com.ecorating.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Статус фонового импорта справочника промышленности (data.mos.ru, напр. 2601) — для /debug/warmup-status. */
public record IndustryWarmupStatusDto(
        @JsonProperty("import_enabled")
        boolean importEnabled,
        @JsonProperty("dataset_id")
        int datasetId,
        @JsonProperty("import_in_progress")
        boolean importInProgress,
        @JsonProperty("records_imported")
        Integer recordsImported,
        @JsonProperty("api_rows_fetched")
        Integer apiRowsFetched,
        @JsonProperty("summary")
        String summary
) {
}
