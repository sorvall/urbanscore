package com.ecorating.config;

import com.ecorating.service.MoscowIndustryHazardImportService;
import com.ecorating.service.MoscowIndustryHazardImportStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Фоновый импорт справочника промышленности в hazard_objects после старта приложения.
 * Первый прогрев может занять много времени (Nominatim ~1 запрос/с).
 */
@Component
public class MoscowIndustryHazardImportRunner {

    private static final Logger log = LoggerFactory.getLogger(MoscowIndustryHazardImportRunner.class);

    private final MoscowIndustryHazardImportService importService;
    private final MoscowIndustryHazardImportStatus importStatus;
    private final boolean importEnabled;

    public MoscowIndustryHazardImportRunner(
            MoscowIndustryHazardImportService importService,
            MoscowIndustryHazardImportStatus importStatus,
            @Value("${external.mos-data.industry-import-enabled:false}") boolean importEnabled
    ) {
        this.importService = importService;
        this.importStatus = importStatus;
        this.importEnabled = importEnabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleImport() {
        if (!importEnabled) {
            return;
        }
        importStatus.markRunning();
        Thread t = new Thread(() -> {
            try {
                importService.importFromApi();
            } catch (Exception ex) {
                log.warn("Industrial hazards import failed: {}", ex.getMessage());
                importStatus.markUnexpectedFailure(ex.getMessage());
            }
        }, "mos-industry-hazard-import");
        t.setDaemon(true);
        t.start();
    }
}
