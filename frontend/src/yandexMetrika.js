/**
 * Яндекс.Метрика: счётчик из VITE_YM_ID, цели — для проверки адреса (геокод / клик по карте).
 * В интерфейсе Метрики создайте JavaScript-цели с теми же идентификаторами.
 */
const YM_ID_RAW = import.meta.env.VITE_YM_ID;

/** Успешный геокод по строке (GET /api/v1/geocode). */
export const YM_GOAL_ADDRESS_CHECK_MANUAL_OK = 'address_check_manual_ok';
/** Ошибка геокода по строке. */
export const YM_GOAL_ADDRESS_CHECK_MANUAL_FAIL = 'address_check_manual_fail';
/** Успешное определение адреса по координатам (GET /api/v1/address). */
export const YM_GOAL_ADDRESS_CHECK_MAP_OK = 'address_check_map_ok';
/** Ошибка обратного геокодирования по карте. */
export const YM_GOAL_ADDRESS_CHECK_MAP_FAIL = 'address_check_map_fail';

let initDone = false;

/**
 * Подключает tag.js и ym('init', …). Без VITE_YM_ID — no-op.
 */
export function initYandexMetrika() {
  if (initDone || typeof window === 'undefined') {
    return;
  }
  const id = String(YM_ID_RAW ?? '').trim();
  if (!/^\d+$/.test(id)) {
    return;
  }
  initDone = true;

  (function loadYm(m, e, t, r, i, k, a) {
    m[i] =
      m[i] ||
      function yandexMetrikaStub() {
        (m[i].a = m[i].a || []).push(arguments);
      };
    m[i].l = 1 * new Date();
    for (let j = 0; j < document.scripts.length; j += 1) {
      if (document.scripts[j].src === r) {
        return;
      }
    }
    k = e.createElement(t);
    a = e.getElementsByTagName(t)[0];
    k.async = 1;
    k.src = r;
    a.parentNode.insertBefore(k, a);
  })(window, document, 'script', 'https://mc.yandex.ru/metrika/tag.js', 'ym');

  window.ym(id, 'init', {
    clickmap: true,
    trackLinks: true,
    accurateTrackBounce: true
  });
}

/**
 * @param {string} goalName — идентификатор цели в Метрике
 * @param {Record<string, unknown>} [params] — параметры визита (опционально)
 */
export function ymReachGoal(goalName, params) {
  if (typeof window === 'undefined' || !goalName) {
    return;
  }
  const id = String(YM_ID_RAW ?? '').trim();
  if (!/^\d+$/.test(id) || typeof window.ym !== 'function') {
    return;
  }
  try {
    window.ym(id, 'reachGoal', goalName, params);
  } catch {
    /* ignore */
  }
}
