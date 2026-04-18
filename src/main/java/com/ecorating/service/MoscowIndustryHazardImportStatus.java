package com.ecorating.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Состояние фонового импорта промышленности (набор 2601) для отладки и {@link WarmupStatusService}.
 */
@Component
public class MoscowIndustryHazardImportStatus {

    private final boolean importEnabledConfigured;
    private final int datasetId;

    private final AtomicBoolean importInProgress = new AtomicBoolean(false);
    private final AtomicInteger recordsImported = new AtomicInteger(-1);
    private final AtomicInteger apiRowsFetched = new AtomicInteger(-1);
    private final AtomicReference<String> summary = new AtomicReference<>("");

    public MoscowIndustryHazardImportStatus(
            @Value("${external.mos-data.industry-import-enabled:false}") boolean importEnabledConfigured,
            @Value("${external.mos-data.industry-dataset-id:2601}") int datasetId
    ) {
        this.importEnabledConfigured = importEnabledConfigured;
        this.datasetId = datasetId;
        if (!importEnabledConfigured) {
            summary.set("Импорт промышленности отключён (MOS_DATA_INDUSTRY_IMPORT_ENABLED=false).");
        } else {
            summary.set("Ожидание фонового импорта после старта приложения…");
        }
    }

    public boolean isImportEnabledConfigured() {
        return importEnabledConfigured;
    }

    public int getDatasetId() {
        return datasetId;
    }

    public boolean isImportInProgress() {
        return importInProgress.get();
    }

    public Integer getRecordsImported() {
        int v = recordsImported.get();
        return v < 0 ? null : v;
    }

    public Integer getApiRowsFetched() {
        int v = apiRowsFetched.get();
        return v < 0 ? null : v;
    }

    public String getSummary() {
        return summary.get();
    }

    public void markRunning() {
        importInProgress.set(true);
        recordsImported.set(-1);
        apiRowsFetched.set(-1);
        summary.set("Идёт импорт набора " + datasetId + " (data.mos.ru) и геокодирование адресов…");
    }

    public void markApiFailure(String message) {
        importInProgress.set(false);
        recordsImported.set(-1);
        apiRowsFetched.set(-1);
        summary.set("Импорт не выполнен: " + (message == null ? "ошибка API" : message));
    }

    public void markEmptyGeocode(int apiRows) {
        importInProgress.set(false);
        recordsImported.set(0);
        apiRowsFetched.set(apiRows);
        summary.set(
                "Строк из API: "
                        + apiRows
                        + ", с координатами: 0 — прежние записи справочника в БД не менялись (геокодинг не дал точек)."
        );
    }

    public void markSuccess(int imported, int apiRows) {
        importInProgress.set(false);
        recordsImported.set(imported);
        apiRowsFetched.set(apiRows);
        summary.set(
                "Импорт завершён: "
                        + imported
                        + " записей с координатами из "
                        + apiRows
                        + " строк API (набор "
                        + datasetId
                        + ")."
        );
    }

    public void markUnexpectedFailure(String message) {
        importInProgress.set(false);
        summary.set("Ошибка импорта: " + (message == null ? "неизвестно" : message));
    }
}
