import { useCallback, useState } from 'react';
import { MapContainer, TileLayer, Marker, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { DEFAULT_EXPERT_PROMPT } from './defaultPrompt.js';
import { prepareReportHtml } from './prepareReportHtml.js';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const INITIAL_PROMPT = import.meta.env.VITE_DEFAULT_PROMPT || DEFAULT_EXPERT_PROMPT;

const MOSCOW = [55.751244, 37.618423];

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

export default function App() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [address, setAddress] = useState('');
  const [html, setHtml] = useState('');
  const [marker, setMarker] = useState(null);

  const onMapClick = useCallback(
    async (lat, lon) => {
      setMarker([lat, lon]);
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
          setError(addrPayload.error || `Не удалось определить адрес (${addrRes.status})`);
          return;
        }
        const resolvedAddress = addrPayload.data.address;

        const res = await fetch(`${API_BASE_URL}/api/v1/report`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            address: resolvedAddress,
            prompt: INITIAL_PROMPT
          })
        });
        const payload = await res.json().catch(() => ({}));
        if (!res.ok || !payload.success) {
          setError(payload.error || `Ошибка ${res.status}`);
          return;
        }
        setAddress(payload.data?.address ?? resolvedAddress);
        setHtml(payload.data?.html ?? '');
      } catch (e) {
        setError(e?.message || 'Сеть недоступна');
      } finally {
        setLoading(false);
      }
    },
    []
  );

  return (
    <div className="shell">
      <header className="header">
        <div className="header-brand">
          <span className="logo-mark" aria-hidden="true" />
          <div>
            <h1 className="header-title">UrbanScore</h1>
            <p className="header-sub">Клик по карте — адрес и отчёт по району</p>
          </div>
        </div>
      </header>

      <main className="main">
        <section className="map-section" aria-label="Карта Москвы">
          <div className="map-shell">
            <MapContainer center={MOSCOW} zoom={11} className="map" attributionControl={false}>
              <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
              <MapClickHandler onSelect={onMapClick} />
              {marker ? <Marker position={marker} icon={markerIcon} /> : null}
            </MapContainer>
            {loading ? (
              <div className="map-loading" aria-busy="true" aria-live="polite">
                <div className="spinner" />
              </div>
            ) : null}
          </div>
        </section>

        {error ? (
          <div className="alert alert--error" role="alert">
            {error}
          </div>
        ) : null}

        {address ? (
          <div className="address-card">
            <span className="address-card-label">Адрес</span>
            <p className="address-card-text">{address}</p>
          </div>
        ) : null}

        {html ? (
          <article
            className="report-html"
            dangerouslySetInnerHTML={{ __html: prepareReportHtml(html) }}
          />
        ) : null}
      </main>
    </div>
  );
}
