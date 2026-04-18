-- Внешний идентификатор записи data.mos.ru для импорта справочников (напр. набор 2601)
ALTER TABLE hazard_objects
    ADD COLUMN source_global_id BIGINT NULL;

CREATE UNIQUE INDEX uq_hazard_objects_source_global
    ON hazard_objects (source, source_global_id)
    WHERE source_global_id IS NOT NULL;
