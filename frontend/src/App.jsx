import { useEffect, useMemo, useState } from 'react';
import { MapContainer, Marker, TileLayer, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
/** Отключить: VITE_SHOW_WARMUP_DEBUG=false */
const SHOW_WARMUP_DEBUG = import.meta.env.VITE_SHOW_WARMUP_DEBUG !== 'false';
const MOSCOW_CENTER = [55.751244, 37.618423];

const markerIcon = L.icon({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41]
});

function MapClickHandler({ onSelect }) {
  useMapEvents({
    click(event) {
      onSelect(event.latlng.lat, event.latlng.lng);
    }
  });
  return null;
}

function formatDateTime(value) {
  if (!value) return 'Нет данных';
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return new Intl.DateTimeFormat('ru-RU', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(parsed);
}

function formatNumber(value, digits = 1) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return 'Нет данных';
  }
  return Number(value).toFixed(digits);
}

function getAqiTone(aqi) {
  if (aqi <= 50) return 'aqi-good';
  if (aqi <= 100) return 'aqi-moderate';
  if (aqi <= 150) return 'aqi-sensitive';
  if (aqi <= 200) return 'aqi-unhealthy';
  if (aqi <= 300) return 'aqi-very-unhealthy';
  return 'aqi-hazardous';
}

function getScoreLabel(score) {
  if (score >= 80) return { text: 'Отлично', tone: 'good' };
  if (score >= 60) return { text: 'Хорошо', tone: 'ok' };
  if (score >= 40) return { text: 'Средне', tone: 'warn' };
  return { text: 'Низко', tone: 'bad' };
}

export default function App() {
  const [selectedPoint, setSelectedPoint] = useState(null);
  const [report, setReport] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [debugExchange, setDebugExchange] = useState(null);
  const [warmupStatus, setWarmupStatus] = useState(null);

  useEffect(() => {
    if (!loading || !SHOW_WARMUP_DEBUG) {
      return undefined;
    }
    let cancelled = false;
    const load = async () => {
      try {
        const r = await fetch(`${API_BASE_URL}/api/v1/debug/warmup-status`);
        if (!r.ok) return;
        const payload = await r.json();
        if (!cancelled && payload?.success && payload?.data) {
          setWarmupStatus(payload.data);
        }
      } catch {
        if (!cancelled) setWarmupStatus(null);
      }
    };
    load();
    const id = setInterval(load, 1500);
    return () => {
      cancelled = true;
      clearInterval(id);
    };
  }, [loading, SHOW_WARMUP_DEBUG]);

  const apiRequestParams = useMemo(() => {
    if (!selectedPoint) {
      return null;
    }
    const { lat, lon } = selectedPoint;
    const queryString = `lat=${encodeURIComponent(lat)}&lon=${encodeURIComponent(lon)}`;
    const path = `/api/v1/eco?${queryString}`;
    return {
      lat,
      lon,
      latFormatted: lat.toFixed(6),
      lonFormatted: lon.toFixed(6),
      queryString,
      fullUrl: `${API_BASE_URL}${path}`
    };
  }, [selectedPoint]);

  const fetchEcoReport = async (lat, lon) => {
    setSelectedPoint({ lat, lon });
    setError('');
    setWarmupStatus(null);
    setLoading(true);

    const fullUrl = `${API_BASE_URL}/api/v1/eco?lat=${encodeURIComponent(lat)}&lon=${encodeURIComponent(lon)}`;
    const requestPayload = {
      method: 'GET',
      url: fullUrl,
      query: { lat, lon }
    };

    let exchangeResponse = null;

    try {
      const response = await fetch(fullUrl);
      const rawText = await response.text();
      let payload = null;
      try {
        payload = rawText ? JSON.parse(rawText) : null;
      } catch {
        exchangeResponse = {
          httpStatus: response.status,
          ok: response.ok,
          bodyParseError: 'Ответ не является JSON',
          rawBodyPreview: rawText.slice(0, 4000)
        };
        setDebugExchange({
          request: requestPayload,
          response: exchangeResponse
        });
        setReport(null);
        setError('Ответ сервера не JSON');
        return;
      }

      exchangeResponse = {
        httpStatus: response.status,
        ok: response.ok,
        body: payload
      };
      setDebugExchange({
        request: requestPayload,
        response: exchangeResponse
      });

      if (!response.ok) {
        setReport(null);
        setError(`HTTP ${response.status}`);
        return;
      }

      if (!payload.success) {
        throw new Error(payload.error || 'Сервер вернул ошибку');
      }

      setReport(payload.data);
    } catch (requestError) {
      setReport(null);
      setError(requestError.message || 'Не удалось получить данные');
      setDebugExchange({
        request: requestPayload,
        response: exchangeResponse,
        error: requestError.message || String(requestError)
      });
    } finally {
      setLoading(false);
      if (SHOW_WARMUP_DEBUG) {
        try {
          const r = await fetch(`${API_BASE_URL}/api/v1/debug/warmup-status`);
          if (r.ok) {
            const payload = await r.json();
            if (payload?.success && payload?.data) {
              setWarmupStatus(payload.data);
            }
          }
        } catch {
          /* ignore */
        }
      }
    }
  };

  const summaryBadge = report ? getScoreLabel(report.overallEcoScore) : null;

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>UrbanScore</h1>
          <p>Интерактивная карта экологической оценки городской среды</p>
        </div>
      </header>

      <div className="layout">
      <section className="map-panel">
        <div className="panel-heading">
          <h2>Карта</h2>
          <p>Выберите точку на карте, чтобы рассчитать индекс района.</p>
        </div>

        <MapContainer
          center={MOSCOW_CENTER}
          zoom={12}
          className="map"
          attributionControl={false}
        >
          <TileLayer
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />

          <MapClickHandler onSelect={fetchEcoReport} />

          {selectedPoint && (
            <Marker
              icon={markerIcon}
              position={[selectedPoint.lat, selectedPoint.lon]}
            />
          )}
        </MapContainer>
        <p className="map-attribution">
          Карта: © участники{' '}
          <a
            href="https://www.openstreetmap.org/copyright"
            target="_blank"
            rel="noreferrer noopener"
          >
            OpenStreetMap
          </a>
        </p>
      </section>

      <aside className="info-panel">
        <div className="panel-heading">
          <h2>Результат</h2>
          {summaryBadge && (
            <span className={`badge badge-${summaryBadge.tone}`}>{summaryBadge.text}</span>
          )}
        </div>

        {!selectedPoint && <p className="hint">Выберите точку на карте слева.</p>}

        {apiRequestParams && (
          <div className="api-params">
            <div className="api-params-title">Запрос к API</div>
            <ul className="api-params-list">
              <li>
                <span>Широта (lat)</span>
                <strong>{apiRequestParams.latFormatted}</strong>
              </li>
              <li>
                <span>Долгота (lon)</span>
                <strong>{apiRequestParams.lonFormatted}</strong>
              </li>
              <li className="api-params-query">
                <span>Параметры в URL</span>
                <code>{apiRequestParams.queryString}</code>
              </li>
              <li className="api-params-query">
                <span>Полный адрес</span>
                <code className="api-params-url">{apiRequestParams.fullUrl}</code>
              </li>
            </ul>
          </div>
        )}

        {loading && <p className="hint">Загружаю данные...</p>}
        {SHOW_WARMUP_DEBUG && warmupStatus?.landscaping && (
          <div className="warmup-debug" role="status" aria-live="polite">
            <div className="warmup-debug-title">Прогрев кэша (отладка)</div>
            <p className="warmup-debug-summary">{warmupStatus.landscaping.summary}</p>
            <ul className="warmup-debug-list">
              <li>
                <span>Сейчас выполняется</span>
                <strong>{warmupStatus.landscaping.warmup_in_progress ? 'да' : 'нет'}</strong>
              </li>
              <li>
                <span>Снимок сырых строк</span>
                <strong>{warmupStatus.landscaping.rows_snapshot_present ? 'есть' : 'нет'}</strong>
              </li>
              <li>
                <span>Снимок точек (геокод)</span>
                <strong>{warmupStatus.landscaping.points_snapshot_present ? 'есть' : 'нет'}</strong>
              </li>
            </ul>
          </div>
        )}
        {error && <p className="error">Ошибка: {error}</p>}

        {report && !loading && !error && (
          <p className="success" role="status">
            Данные успешно получены
          </p>
        )}

        {report && (
          <div className="report">
            <div className="metric-grid">
              <div className="metric-card">
                <span>Качество воздуха</span>
                <strong>{report.airQualityIndex.toFixed(1)}</strong>
              </div>
              <div className="metric-card">
                <span>Зеленые зоны</span>
                <strong>{report.greenZoneIndex.toFixed(1)}</strong>
              </div>
              <div className="metric-card">
                <span>Риски</span>
                <strong>{report.hazardIndex.toFixed(1)}</strong>
              </div>
              <div className="metric-card metric-primary">
                <span>Общий эко-скор</span>
                <strong>{report.overallEcoScore.toFixed(1)}</strong>
              </div>
            </div>
            <div className="meta">
              <span>Обновлено:</span>
              <strong>{formatDateTime(report.dataFreshnessTimestamp)}</strong>
            </div>

            {report.airQualitySource && (
              <div className="meta meta-secondary">
                <span>Источник данных по воздуху:</span>
                <strong>{report.airQualitySource}</strong>
              </div>
            )}

            {report.aqicnData && (
              <>
                <h3>Данные AQICN</h3>
                <p className="aqicn-hint">
                  Пункт ниже — тот, который вернул официальный JSON API WAQI для ваших координат. На сайте
                  aqicn.org на карте может быть выбран другой пост (другой набор станций в интерфейсе); для
                  одной и той же области API часто отдаёт одну привязанную станцию.
                </p>
                <ul className="item-list">
                  <li className={getAqiTone(report.aqicnData.aqi)}>
                    <span>Индекс качества воздуха (AQI)</span>
                    <strong>{formatNumber(report.aqicnData.aqi, 0)}</strong>
                  </li>
                  <li>
                    <span>Доминирующий загрязнитель</span>
                    <strong>{report.aqicnData.dominant_pollutant || 'Нет данных'}</strong>
                  </li>
                  <li>
                    <span>PM2.5</span>
                    <strong>{formatNumber(report.aqicnData.pm25)} мкг/м3</strong>
                  </li>
                  <li>
                    <span>PM10</span>
                    <strong>{formatNumber(report.aqicnData.pm10)} мкг/м3</strong>
                  </li>
                  <li>
                    <span>Температура</span>
                    <strong>{formatNumber(report.aqicnData.temperature)} °C</strong>
                  </li>
                  <li>
                    <span>Влажность</span>
                    <strong>{formatNumber(report.aqicnData.humidity, 0)} %</strong>
                  </li>
                  <li>
                    <span>Скорость ветра</span>
                    <strong>{formatNumber(report.aqicnData.wind_speed)} м/с</strong>
                  </li>
                  <li>
                    <span>Пункт измерения (ответ API)</span>
                    <strong>{report.aqicnData.station_name || 'Нет данных'}</strong>
                  </li>
                  <li>
                    <span>Время последнего обновления</span>
                    <strong>{formatDateTime(report.aqicnData.last_update)}</strong>
                  </li>
                </ul>
              </>
            )}

            <h3>Ближайшие источники риска</h3>
            {report.nearestHazards?.length ? (
              <ul className="item-list">
                {report.nearestHazards.map((item, idx) => (
                  <li key={`${item.name}-${idx}`}>
                    <span>{item.name} ({item.type})</span>
                    <strong>{item.distanceMeters.toFixed(0)} м</strong>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="hint">Рядом не найдено объектов риска.</p>
            )}

            <h3>Ближайшие парки и леса</h3>
            {report.nearestParks?.length ? (
              <ul className="item-list">
                {report.nearestParks.map((item, idx) => (
                  <li key={`${item.name}-${idx}`}>
                    <span>{item.name}</span>
                    <strong>{item.distanceMeters.toFixed(0)} м</strong>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="hint">Рядом нет данных о зеленых зонах.</p>
            )}

            <h3>Городской Wi‑Fi (Москва)</h3>
            {report.nearest_city_wifi?.length ? (
              <div className="wifi-card-list">
                {report.nearest_city_wifi.map((wifi, idx) => (
                  <article
                    key={`${wifi.latitude}-${wifi.longitude}-${idx}`}
                    className="wifi-card"
                  >
                    <div className="wifi-card-head">
                      <span className="wifi-rank">{idx + 1}</span>
                      <h4 className="wifi-title">{wifi.name || 'Городской Wi‑Fi'}</h4>
                    </div>
                    {wifi.address ? (
                      <p className="wifi-address">{wifi.address}</p>
                    ) : (
                      <p className="wifi-address wifi-address-missing">Адрес в наборе данных не указан</p>
                    )}
                    <ul className="item-list wifi-meta">
                      {wifi.network_name && (
                        <li>
                          <span>Сеть (SSID)</span>
                          <strong>{wifi.network_name}</strong>
                        </li>
                      )}
                      <li>
                        <span>Расстояние</span>
                        <strong>{formatNumber(wifi.distance_meters, 0)} м</strong>
                      </li>
                    </ul>
                  </article>
                ))}
                <p className="meta meta-secondary wifi-source">
                  <span>Источник данных:</span>
                  <strong>{report.nearest_city_wifi[0]?.source || 'data.mos.ru'}</strong>
                </p>
              </div>
            ) : (
              <p className="hint">
                Нет данных по Wi‑Fi: задайте в <code className="hint-code">.env</code> на бэкенде{' '}
                <code className="hint-code">MOS_DATA_API_KEY</code> (тот же ключ, что для других наборов data.mos.ru,
                например кинотеатров) и выберите точку в пределах Москвы. Набор по умолчанию — 2756.
              </p>
            )}

            <h3>Кинотеатры (справочник Москвы)</h3>
            {report.nearest_cinemas?.length ? (
              <div className="wifi-card-list">
                {report.nearest_cinemas.map((cinema, idx) => (
                  <article
                    key={`${cinema.latitude}-${cinema.longitude}-${idx}`}
                    className="wifi-card"
                  >
                    <div className="wifi-card-head">
                      <span className="wifi-rank">{idx + 1}</span>
                      <h4 className="wifi-title">{cinema.name || 'Кинотеатр'}</h4>
                    </div>
                    {cinema.full_name && (
                      <p className="cinema-full-name">{cinema.full_name}</p>
                    )}
                    {cinema.address ? (
                      <p className="wifi-address">{cinema.address}</p>
                    ) : (
                      <p className="wifi-address wifi-address-missing">Адрес в справочнике не указан</p>
                    )}
                    <ul className="item-list wifi-meta">
                      {cinema.category && (
                        <li>
                          <span>Категория</span>
                          <strong>{cinema.category}</strong>
                        </li>
                      )}
                      {cinema.phone && (
                        <li>
                          <span>Телефон</span>
                          <strong>{cinema.phone}</strong>
                        </li>
                      )}
                      <li>
                        <span>Расстояние</span>
                        <strong>{formatNumber(cinema.distance_meters, 0)} м</strong>
                      </li>
                    </ul>
                  </article>
                ))}
                <p className="meta meta-secondary wifi-source">
                  <span>Источник данных:</span>
                  <strong>{report.nearest_cinemas[0]?.source || 'data.mos.ru'}</strong>
                </p>
              </div>
            ) : (
              <p className="hint">
                Нет данных по кинотеатрам: для портала data.mos.ru используется тот же ключ{' '}
                <code className="hint-code">MOS_DATA_API_KEY</code>, что и для Wi‑Fi. Проверьте, что он задан в{' '}
                <code className="hint-code">.env</code> на бэкенде, и что точка на карте в пределах Москвы (набор 495 в
                API).
              </p>
            )}

            <h3>Благоустройство (жилая застройка)</h3>
            {report.nearest_landscaping_works?.length ? (
              <div className="wifi-card-list">
                {report.nearest_landscaping_works.map((work, idx) => (
                  <article
                    key={`${work.latitude}-${work.longitude}-${idx}-${work.work_name}`}
                    className="wifi-card"
                  >
                    <div className="wifi-card-head">
                      <span className="wifi-rank">{idx + 1}</span>
                      <h4 className="wifi-title">{work.work_name || 'Работа по благоустройству'}</h4>
                    </div>
                    {work.work_essence && (
                      <p className="cinema-full-name">{work.work_essence}</p>
                    )}
                    {work.address ? (
                      <p className="wifi-address">{work.address}</p>
                    ) : (
                      <p className="wifi-address wifi-address-missing">Адрес в справочнике не указан</p>
                    )}
                    <ul className="item-list wifi-meta">
                      {work.district && (
                        <li>
                          <span>Район</span>
                          <strong>{work.district}</strong>
                        </li>
                      )}
                      {work.year_of_work != null && (
                        <li>
                          <span>Год работ</span>
                          <strong>{work.year_of_work}</strong>
                        </li>
                      )}
                      {work.work_status && (
                        <li>
                          <span>Статус</span>
                          <strong>{work.work_status}</strong>
                        </li>
                      )}
                      {work.volume && (
                        <li>
                          <span>Объём</span>
                          <strong>{work.volume}</strong>
                        </li>
                      )}
                      <li>
                        <span>Расстояние</span>
                        <strong>{formatNumber(work.distance_meters, 0)} м</strong>
                      </li>
                    </ul>
                  </article>
                ))}
                <p className="meta meta-secondary wifi-source">
                  <span>Источник данных:</span>
                  <strong>{report.nearest_landscaping_works[0]?.source || 'data.mos.ru'}</strong>
                </p>
              </div>
            ) : (
              <p className="hint">
                Нет данных по благоустройству: нужен <code className="hint-code">MOS_DATA_API_KEY</code>, точка в Москве,
                и загрузка набора 62961 (год не ниже заданного в{' '}
                <code className="hint-code">MOS_DATA_LANDSCAPING_MIN_YEAR</code>). Первый запрос после деплоя может
                занять много времени (геокодинг адресов).
              </p>
            )}
          </div>
        )}
      </aside>
    </div>

      {debugExchange && (
        <section className="debug-panel" aria-label="Отладка запроса и ответа">
          <h2 className="debug-panel-title">Отладка (запрос и ответ)</h2>
          <p className="debug-panel-note">
            Временный блок для проверки интеграции: что ушло на бэкенд и полный JSON ответа.
          </p>
          <div className="debug-json-grid">
            <div>
              <h3 className="debug-json-heading">Отправили</h3>
              <pre className="debug-json">
                {JSON.stringify(debugExchange.request, null, 2)}
              </pre>
            </div>
            <div>
              <h3 className="debug-json-heading">Получили</h3>
              {debugExchange.error && (
                <p className="debug-json-error">Ошибка: {debugExchange.error}</p>
              )}
              <pre className="debug-json">
                {JSON.stringify(
                  debugExchange.response !== undefined
                    ? debugExchange.response
                    : null,
                  null,
                  2
                )}
              </pre>
            </div>
          </div>
        </section>
      )}
    </div>
  );
}
