INSERT INTO hazard_objects (name, hazard_type, location, description, source)
VALUES
    (
        'TPP-12 Demo',
        'TPP',
        ST_SetSRID(ST_MakePoint(37.635500, 55.754100), 4326)::geography,
        'Demo thermal power plant record for local development.',
        'SEED'
    ),
    (
        'MSW Transfer Station Demo',
        'LANDFILL',
        ST_SetSRID(ST_MakePoint(37.600500, 55.741900), 4326)::geography,
        'Demo landfill-related hazard object for scoring calibration.',
        'SEED'
    ),
    (
        'Industrial Plant Demo',
        'FACTORY',
        ST_SetSRID(ST_MakePoint(37.622400, 55.766300), 4326)::geography,
        'Demo factory object near central district.',
        'SEED'
    );

INSERT INTO green_zones (osm_id, name, zone_type, location, area_m2)
VALUES
    (
        990000001,
        'Alexandrovsky Garden Demo',
        'PARK',
        ST_SetSRID(ST_MakePoint(37.610800, 55.752100), 4326)::geography,
        120000
    ),
    (
        990000002,
        'Tverskoy Boulevard Demo',
        'SQUARE',
        ST_SetSRID(ST_MakePoint(37.602700, 55.760700), 4326)::geography,
        45000
    ),
    (
        990000003,
        'Neskuchny Forest Demo',
        'FOREST',
        ST_SetSRID(ST_MakePoint(37.592200, 55.718900), 4326)::geography,
        240000
    );
