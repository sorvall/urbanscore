package com.ecorating.service;

import com.ecorating.client.MoscowOpenDataClient;
import com.ecorating.client.NominatimGeocoder;
import com.ecorating.exception.ExternalApiException;
import com.ecorating.model.HazardObject;
import com.ecorating.repository.HazardObjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Импорт справочника промышленных предприятий (data.mos.ru, напр. набор 2601) в {@code hazard_objects}.
 */
@Service
public class MoscowIndustryHazardImportService {

    private static final Logger log = LoggerFactory.getLogger(MoscowIndustryHazardImportService.class);

    private final MoscowOpenDataClient moscowOpenDataClient;
    private final NominatimGeocoder nominatimGeocoder;
    private final HazardObjectRepository hazardObjectRepository;
    private final MoscowIndustryHazardImportStatus importStatus;

    private final int datasetId;
    private final int maxRows;

    public MoscowIndustryHazardImportService(
            MoscowOpenDataClient moscowOpenDataClient,
            NominatimGeocoder nominatimGeocoder,
            HazardObjectRepository hazardObjectRepository,
            MoscowIndustryHazardImportStatus importStatus,
            @Value("${external.mos-data.industry-dataset-id:2601}") int datasetId,
            @Value("${external.mos-data.industry-max-rows:2000}") int maxRows
    ) {
        this.moscowOpenDataClient = moscowOpenDataClient;
        this.nominatimGeocoder = nominatimGeocoder;
        this.hazardObjectRepository = hazardObjectRepository;
        this.importStatus = importStatus;
        this.datasetId = datasetId;
        this.maxRows = maxRows;
    }

    /**
     * Скачивает строки API, геокодирует адреса без координат, заменяет в БД записи с тем же {@code source}.
     */
    @Transactional
    public synchronized void importFromApi() {
        List<JsonNode> rows;
        try {
            rows = moscowOpenDataClient.fetchRows(datasetId, null, maxRows);
        } catch (ExternalApiException ex) {
            log.warn("Industrial hazards import skipped: {}", ex.getMessage());
            importStatus.markApiFailure(ex.getMessage());
            return;
        }

        List<HazardObject> hazards = new ArrayList<>();
        for (JsonNode row : rows) {
            MoscowIndustrialCatalogRow.toHazardObject(row, nominatimGeocoder).ifPresent(hazards::add);
        }

        List<HazardObject> uniqueHazards = dedupeBySourceGlobalId(hazards);
        if (hazards.size() > uniqueHazards.size()) {
            log.info(
                    "Industrial hazards: skipped {} duplicate source_global_id rows (API / parsing)",
                    hazards.size() - uniqueHazards.size()
            );
        }

        if (uniqueHazards.isEmpty()) {
            log.warn(
                    "Industrial hazards dataset {}: no rows with coordinates; existing records for {} are unchanged",
                    datasetId,
                    MoscowIndustrialCatalogRow.SOURCE_DATASET_2601
            );
            importStatus.markEmptyGeocode(rows.size());
            return;
        }

        hazardObjectRepository.deleteAllBySource(MoscowIndustrialCatalogRow.SOURCE_DATASET_2601);
        // Иначе Hibernate может отправить INSERT раньше DELETE — конфликт uq_hazard_objects_source_global
        hazardObjectRepository.flush();
        hazardObjectRepository.saveAll(uniqueHazards);

        importStatus.markSuccess(uniqueHazards.size(), rows.size());

        log.info(
                "Industrial hazards: imported {} records from data.mos.ru dataset {} (API rows={}, maxRows={})",
                uniqueHazards.size(),
                datasetId,
                rows.size(),
                maxRows
        );
    }

    /**
     * Один и тот же {@code global_id} иногда встречается в ответе API дважды — уникальный индекс (source, source_global_id).
     */
    private static List<HazardObject> dedupeBySourceGlobalId(List<HazardObject> hazards) {
        Map<Long, HazardObject> byId = new LinkedHashMap<>();
        List<HazardObject> withoutGlobalId = new ArrayList<>();
        for (HazardObject h : hazards) {
            Long gid = h.getSourceGlobalId();
            if (gid == null) {
                withoutGlobalId.add(h);
                continue;
            }
            byId.merge(gid, h, (a, b) -> a);
        }
        List<HazardObject> out = new ArrayList<>(byId.values());
        out.addAll(withoutGlobalId);
        return out;
    }
}
