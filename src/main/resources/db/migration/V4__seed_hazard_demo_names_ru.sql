-- Русские подписи для демо-объектов риска (уже развёрнутые БД с англ. V2)
UPDATE hazard_objects
SET name = 'Демо: теплоэлектроцентраль',
    description = 'Тестовая запись для локальной разработки.'
WHERE source = 'SEED'
  AND hazard_type = 'TPP';

UPDATE hazard_objects
SET name = 'Демо: объект обращения с отходами',
    description = 'Тестовая запись для калибровки оценки.'
WHERE source = 'SEED'
  AND hazard_type = 'LANDFILL';

UPDATE hazard_objects
SET name = 'Демо: промышленное предприятие',
    description = 'Тестовая запись рядом с центром.'
WHERE source = 'SEED'
  AND hazard_type = 'FACTORY';
