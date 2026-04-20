import { useCallback, useEffect, useLayoutEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, useMap, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { prepareReportHtml } from './prepareReportHtml.js';
import {
  ymReachGoal,
  YM_GOAL_ADDRESS_CHECK_MANUAL_FAIL,
  YM_GOAL_ADDRESS_CHECK_MANUAL_OK,
  YM_GOAL_ADDRESS_CHECK_MAP_FAIL,
  YM_GOAL_ADDRESS_CHECK_MAP_OK
} from './yandexMetrika.js';

/** Пустая строка = тот же origin (прокси /api на бэкенд, например Caddy). */
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

const MOSCOW = [55.751244, 37.618423];

/** Полоса — ориентир ожидания (согласовано с типичным DEEPSEEK_RESPONSE_TIMEOUT на бэкенде). */
const REPORT_PROGRESS_MS = 300_000;

const REPORT_LOAD_PHRASE_INTERVAL_MS = 8_000;

const REPORT_LOAD_PHRASES = [
  'Проверяем юридическую чистоту объекта в Росреестре…',
  'Ищем данные о застройщике и его банкротствах…',
  'Смотрим актуальные цены на квартиры в районе…',
  'Проверяем ближайшие станции метро и время пешком…',
  'Анализируем пробки на ближайших шоссе…',
  'Ищем школы и детские сады с рейтингами…',
  'Запрашиваем данные Мосэкомониторинга по экологии…',
  'Сверяемся с картой шума Москвы…',
  'Проверяем статистику преступности в районе…',
  'Ищем планы реновации и КРТ рядом с домом…',
  'Анализируем средний срок экспозиции квартир…',
  'Проверяем наличие коммерции на первых этажах…',
  'Смотрим парковочные места и ситуацию во дворе…',
  'Формируем экспертное заключение с цифрами…',
  'Почти готово — финальные штрихи отчёта…'
];

function shuffleArray(items) {
  const a = [...items];
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}

const markerIcon = L.divIcon({
  className: 'urban-marker-root',
  html: `<div class="urban-marker-pin"><span class="urban-marker-letter">М</span></div>`,
  iconSize: [32, 32],
  iconAnchor: [16, 32],
  popupAnchor: [0, -28]
});

function MapClickHandler({ onSelect, clicksEnabled }) {
  useMapEvents({
    click(event) {
      if (!clicksEnabled) {
        return;
      }
      onSelect(event.latlng.lat, event.latlng.lng);
    }
  });
  return null;
}

/** Пролёт карты к выбранным координатам (ручной адрес или клик). */
function FlyToMap({ position }) {
  const map = useMap();
  useEffect(() => {
    if (!position || position.length !== 2) {
      return;
    }
    const [lat, lon] = position;
    map.flyTo([lat, lon], Math.max(map.getZoom(), 15), { duration: 0.85 });
  }, [position, map]);
  return null;
}

function EmptyReportState() {
  return (
    <div className="empty-state">
      <svg width="70" height="70" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
        <path
          d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"
          fill="#cddae9"
        />
      </svg>
      <p className="empty-state-title">Укажите адрес ниже или кликните по карте</p>
      <p className="empty-state-hint">После этого здесь появится аналитический отчёт</p>
    </div>
  );
}

export default function App() {
  const [loading, setLoading] = useState(false);
  const [loadProgress, setLoadProgress] = useState(0);
  const [error, setError] = useState('');
  const [address, setAddress] = useState('');
  const [addressInput, setAddressInput] = useState('');
  const [html, setHtml] = useState('');
  const [marker, setMarker] = useState(null);
  /** Координаты для flyTo (маркер + центр карты при вводе адреса / клике). */
  const [flyToPosition, setFlyToPosition] = useState(null);
  const [loadingCaption, setLoadingCaption] = useState('');

  useLayoutEffect(() => {
    if (!loading) {
      setLoadingCaption('');
      return undefined;
    }
    const order = shuffleArray(REPORT_LOAD_PHRASES);
    let idx = 0;
    setLoadingCaption(order[0]);
    const id = window.setInterval(() => {
      idx = (idx + 1) % order.length;
      setLoadingCaption(order[idx]);
    }, REPORT_LOAD_PHRASE_INTERVAL_MS);
    return () => window.clearInterval(id);
  }, [loading]);

  useEffect(() => {
    if (!loading) {
      setLoadProgress(0);
      return;
    }
    const start = Date.now();
    const id = window.setInterval(() => {
      const elapsed = Date.now() - start;
      setLoadProgress(Math.min(100, (elapsed / REPORT_PROGRESS_MS) * 100));
    }, 400);
    return () => window.clearInterval(id);
  }, [loading]);

  const resetReport = useCallback(() => {
    setLoading(false);
    setLoadProgress(0);
    setMarker(null);
    setFlyToPosition(null);
    setAddress('');
    setAddressInput('');
    setHtml('');
    setError('');
  }, []);

  const onManualAddressSubmit = useCallback(
    async (e) => {
      e.preventDefault();
      const raw = addressInput.trim();
      if (!raw || loading) {
        return;
      }
      setMarker(null);
      setFlyToPosition(null);
      setLoading(true);
      setError('');
      setAddress('');
      setHtml('');
      try {
        const geoRes = await fetch(
          `${API_BASE_URL}/api/v1/geocode?q=${encodeURIComponent(raw)}`
        );
        const geoPayload = await geoRes.json().catch(() => ({}));
        if (!geoRes.ok || !geoPayload.success || !geoPayload.data) {
          ymReachGoal(YM_GOAL_ADDRESS_CHECK_MANUAL_FAIL);
          setError(geoPayload.error || `Не удалось найти адрес (${geoRes.status})`);
          return;
        }
        ymReachGoal(YM_GOAL_ADDRESS_CHECK_MANUAL_OK);
        const { address: normalized, lat, lon } = geoPayload.data;
        const pos = [lat, lon];
        setMarker(pos);
        setFlyToPosition(pos);
        setAddressInput(normalized);

        const res = await fetch(`${API_BASE_URL}/api/v1/report`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ address: normalized })
        });
        const payload = await res.json().catch(() => ({}));
        if (!res.ok || !payload.success) {
          setError(payload.error || `Ошибка ${res.status}`);
          return;
        }
        setAddress(payload.data?.address ?? normalized);
        setHtml(payload.data?.html ?? '');
      } catch (err) {
        setError(err?.message || 'Сеть недоступна');
      } finally {
        setLoadProgress(100);
        setLoading(false);
      }
    },
    [addressInput, loading]
  );

  const onMapClick = useCallback(
    async (lat, lon) => {
      const pos = [lat, lon];
      setMarker(pos);
      setFlyToPosition(pos);
      setLoading(true);
      setError('');
      setAddress('');
      setHtml('');
      try {
        const addrRes = await fetch(
          `${API_BASE_URL}/api/v1/address?lat=${encodeURIComponent(lat)}&lon=${encodeURIComponent(lon)}`
        );
        const addrPayload = await addrRes.json().catch(() => ({}));
        if (!addrRes.ok || !addrPayload.success || !addrPayload.data?.address) {
          ymReachGoal(YM_GOAL_ADDRESS_CHECK_MAP_FAIL);
          setError(addrPayload.error || `Не удалось определить адрес (${addrRes.status})`);
          return;
        }
        ymReachGoal(YM_GOAL_ADDRESS_CHECK_MAP_OK);
        const resolvedAddress = addrPayload.data.address;
        setAddressInput(resolvedAddress);

        const res = await fetch(`${API_BASE_URL}/api/v1/report`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ address: resolvedAddress })
        });
        const payload = await res.json().catch(() => ({}));
        if (!res.ok || !payload.success) {
          setError(payload.error || `Ошибка ${res.status}`);
          return;
        }
        const finalAddr = payload.data?.address ?? resolvedAddress;
        setAddress(finalAddr);
        setHtml(payload.data?.html ?? '');
        setAddressInput(finalAddr);
      } catch (e) {
        setError(e?.message || 'Сеть недоступна');
      } finally {
        setLoadProgress(100);
        setLoading(false);
      }
    },
    []
  );

  const showEmpty = !loading && !error && !html;

  return (
    <div className="site-wrapper">
      <header className="top-header">
        <div className="container header-inner">
          <div className="logo">
            <h1 className="logo-title">
              МОСДОМЭКСПЕРТ <span className="logo-inline">⚡ рейтинг локаций</span>
            </h1>
            <span className="logo-tagline">аналитика недвижимости по клику</span>
          </div>
          <div className="badge">
            <span className="badge-ico" aria-hidden="true">
              📍
            </span>
            КАРТА → ОТЧЁТ
          </div>
        </div>
      </header>

      <div className="container">
        <div className="hero">
          <h2 className="hero-title">
            Карта <span className="hero-accent">МОСДОМЭКСПЕРТ</span> — выберите локацию
          </h2>
          <p className="hero-text">
            Введите адрес вручную или нажмите на карту — отчёт по инфраструктуре, транспорту, экологии и рынку
            недвижимости.
          </p>
        </div>

        <section className="address-section" aria-label="Ввод адреса">
          <form className="address-panel" onSubmit={onManualAddressSubmit}>
            <label className="address-label" htmlFor="manual-address">
              Адрес
            </label>
            <div className="address-row">
              <input
                id="manual-address"
                name="manualAddress"
                type="text"
                autoComplete="street-address"
                className="address-input"
                placeholder="Например: Москва, Ленинский проспект, 12"
                value={addressInput}
                onChange={(ev) => setAddressInput(ev.target.value)}
                disabled={loading}
              />
              <button type="submit" className="address-submit" disabled={loading || !addressInput.trim()}>
                Отчёт по адресу
              </button>
            </div>
          </form>
        </section>

        <section className="map-section" aria-label="Карта">
          <div className="map-card">
            <div className="map-header">
              <h3 className="map-header-title">
                🗺️ Интерактивная карта <span className="map-header-pill">клик → отчёт</span>
              </h3>
              <button type="button" className="reset-btn" onClick={resetReport}>
                ⟳ Очистить отчёт
              </button>
            </div>
            <div
              className={
                loading ? 'map-shell map-shell--busy map-shell--loading' : 'map-shell'
              }
            >
              <MapContainer center={MOSCOW} zoom={12} className="map" attributionControl={false} scrollWheelZoom>
                <TileLayer
                  attribution=""
                  url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
                  maxZoom={19}
                  maxNativeZoom={19}
                  minZoom={10}
                />
                <MapClickHandler onSelect={onMapClick} clicksEnabled={!loading} />
                <FlyToMap position={flyToPosition} />
                {marker ? <Marker position={marker} icon={markerIcon} /> : null}
              </MapContainer>
              {loading ? (
                <div className="forming-overlay" aria-busy="true" aria-live="polite">
                  <div className="forming-panel">
                    <p className="forming-title">Отчёт формируется…</p>
                    <div
                      className="load-bar-track"
                      role="progressbar"
                      aria-valuemin={0}
                      aria-valuemax={100}
                      aria-valuenow={Math.round(loadProgress)}
                    >
                      <div className="load-bar-fill" style={{ width: `${loadProgress}%` }} />
                    </div>
                    <div className="forming-phrase-slot">
                      <p key={loadingCaption} className="forming-hint forming-hint--phrase">
                        {loadingCaption}
                      </p>
                    </div>
                    <p className="forming-hint forming-hint--sub">
                      Не закрывайте страницу. Отчет появится ниже ...
                    </p>
                  </div>
                </div>
              ) : null}
            </div>
            <div className="map-footer map-footer--cols">
              <span className="map-footer-note">
                Картография: ©{' '}
                <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">
                  OpenStreetMap
                </a>
              </span>
              <span className="map-footer-hint">
                Номера домов и подписи улиц — при сильном приближении (колёсико мыши или «+» на карте).
              </span>
            </div>
          </div>
        </section>

        <section className="report-section">
          <div className="report-card">
            <div className="report-header">
              <div>
                <h3 className="report-header-title">📋 Детальный отчёт об объекте</h3>
                {address ? <p className="report-address">{address}</p> : null}
              </div>
              <div className="report-badge">аналитика МОСДОМЭКСПЕРТ</div>
            </div>
            <div className="report-content">
              {error ? (
                <div className="alert alert--error" role="alert">
                  {error}
                </div>
              ) : null}
              {html ? (
                <article
                  className="report-html"
                  dangerouslySetInnerHTML={{ __html: prepareReportHtml(html) }}
                />
              ) : null}
              {showEmpty ? <EmptyReportState /> : null}
            </div>
          </div>
        </section>
      </div>

      <footer className="site-footer">
        <div className="container">
          <div className="footer-disclaimer">
            <p>
              <strong>© 2026 МОСДОМЭКСПЕРТ — Аналитика недвижимости</strong>
            </p>
            <p>
              Информация, представленная на сайте, включая отчеты по локациям, данные о ценах, инфраструктуре и
              инвестиционной привлекательности, носит исключительно ознакомительный и информационный характер. Мы не
              гарантируем абсолютную точность, актуальность или полноту сведений, полученных на основе открытых источников
              и аналитических моделей.
            </p>
            <p>
              МОСДОМЭКСПЕРТ не является финансовым консультантом, оценщиком или участником сделок купли-продажи
              недвижимости. Все решения о приобретении квартиры или инвестициях должны приниматься вами самостоятельно
              после профессиональной юридической и финансовой проверки. Использование сайта означает ваше согласие с тем,
              что мы не несем ответственности за любые возможные убытки или последствия, связанные с использованием
              представленной информации.
            </p>
            <p>
              Данные об объектах недвижимости (цены, метраж, год постройки и пр.) сгенерированы в демонстрационных целях
              на основе рыночных тенденций и не являются публичной офертой.
            </p>
          </div>
        </div>
      </footer>
    </div>
  );
}
