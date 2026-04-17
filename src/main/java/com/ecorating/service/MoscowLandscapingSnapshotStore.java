package com.ecorating.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Локальные снимки набора 62961: сырые строки API и готовые точки с координатами (после Nominatim).
 * Позволяет один раз скачать и геокодировать, дальше читать с диска без обращения к apidata.mos.ru.
 */
@Component
public class MoscowLandscapingSnapshotStore {

    private static final Logger log = LoggerFactory.getLogger(MoscowLandscapingSnapshotStore.class);

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final Path baseDir;

    public MoscowLandscapingSnapshotStore(
            ObjectMapper objectMapper,
            @Value("${external.mos-data.landscaping-snapshot-enabled:true}") boolean enabled,
            @Value("${external.mos-data.landscaping-snapshot-directory:./data/mos-landscaping-snapshots}") String directory
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        String dir = directory == null || directory.isBlank() ? "./data/mos-landscaping-snapshots" : directory.trim();
        this.baseDir = Path.of(dir).toAbsolutePath().normalize();
    }

    public Optional<List<MoscowLandscapingPoint>> loadPointsIfPresent(int datasetId, int minYear, int maxRows) {
        if (!enabled) {
            return Optional.empty();
        }
        Path file = pointsFile(datasetId, minYear, maxRows);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            List<MoscowLandscapingPoint> list = objectMapper.readValue(
                    file.toFile(),
                    new TypeReference<List<MoscowLandscapingPoint>>() {}
            );
            log.info("Landscaping: loaded {} geocoded points from snapshot {}", list.size(), file);
            return Optional.of(list);
        } catch (IOException e) {
            log.warn("Landscaping: cannot read points snapshot {}, will rebuild: {}", file, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<List<JsonNode>> loadRowsIfPresent(int datasetId, int minYear, int maxRows) {
        if (!enabled) {
            return Optional.empty();
        }
        Path file = rowsFile(datasetId, minYear, maxRows);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            List<JsonNode> list = objectMapper.readValue(
                    file.toFile(),
                    new TypeReference<List<JsonNode>>() {}
            );
            log.info("Landscaping: loaded {} API rows from snapshot {}", list.size(), file);
            return Optional.of(list);
        } catch (IOException e) {
            log.warn("Landscaping: cannot read rows snapshot {}, will re-fetch: {}", file, e.getMessage());
            return Optional.empty();
        }
    }

    public void saveRows(List<JsonNode> rows, int datasetId, int minYear, int maxRows) {
        if (!enabled || rows == null) {
            return;
        }
        Path file = rowsFile(datasetId, minYear, maxRows);
        try {
            Files.createDirectories(file.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), rows);
            log.info("Landscaping: wrote {} rows to {}", rows.size(), file);
        } catch (IOException e) {
            log.warn("Landscaping: failed to save rows snapshot {}: {}", file, e.getMessage());
        }
    }

    public void savePoints(List<MoscowLandscapingPoint> points, int datasetId, int minYear, int maxRows) {
        if (!enabled || points == null) {
            return;
        }
        Path file = pointsFile(datasetId, minYear, maxRows);
        try {
            Files.createDirectories(file.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), points);
            log.info("Landscaping: wrote {} geocoded points to {}", points.size(), file);
        } catch (IOException e) {
            log.warn("Landscaping: failed to save points snapshot {}: {}", file, e.getMessage());
        }
    }

    private Path rowsFile(int datasetId, int minYear, int maxRows) {
        return baseDir.resolve(String.format("landscaping-%d-y%d-n%d.rows.json", datasetId, minYear, maxRows));
    }

    private Path pointsFile(int datasetId, int minYear, int maxRows) {
        return baseDir.resolve(String.format("landscaping-%d-y%d-n%d.points.json", datasetId, minYear, maxRows));
    }

    /** Наличие файлов без чтения содержимого (для /debug/warmup-status). */
    public boolean rowsSnapshotExists(int datasetId, int minYear, int maxRows) {
        if (!enabled) {
            return false;
        }
        return Files.isRegularFile(rowsFile(datasetId, minYear, maxRows));
    }

    public boolean pointsSnapshotExists(int datasetId, int minYear, int maxRows) {
        if (!enabled) {
            return false;
        }
        return Files.isRegularFile(pointsFile(datasetId, minYear, maxRows));
    }
}
