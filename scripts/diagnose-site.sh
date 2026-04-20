#!/usr/bin/env bash
# Запуск на сервере или локально: DNS, порт 443, HTTPS (в т.ч. без hairpin к своему публичному IP).
# Пример: bash scripts/diagnose-site.sh мосдомэксперт.рф
set -euo pipefail

DOMAIN="${1:-мосдомэксперт.рф}"
PUNY="$(python3 -c "import sys; print(sys.argv[1].encode('idna').decode('ascii'))" "$DOMAIN" 2>/dev/null || true)"
A_REC="$(dig @8.8.8.8 +short "$DOMAIN" A | head -1 | tr -d '\r')"
# dig по UTF-8 на части VPS не отдаёт A — повторяем по punycode
if [[ -z "${A_REC// }" && -n "${PUNY:-}" ]]; then
  A_REC="$(dig @8.8.8.8 +short "$PUNY" A | head -1 | tr -d '\r')"
fi

echo "== Домен: $DOMAIN =="
echo "== DNS A (Google 8.8.8.8) =="
echo "${A_REC:-<пусто>}"
echo "== DNS AAAA =="
dig @8.8.8.8 +short "$DOMAIN" AAAA || true
if [[ -n "${PUNY:-}" ]]; then
  dig @8.8.8.8 +short "$PUNY" AAAA || true
fi
if [[ -n "$PUNY" && "$PUNY" != "$DOMAIN" ]]; then
  echo "== Punycode для CADDY_SITE_LABELS (вторая часть после запятой) =="
  echo "$PUNY"
fi

echo "== Кто слушает TCP 443 (на этой машине) =="
if command -v ss >/dev/null 2>&1; then
  ss -tlnp '( sport = :443 )' 2>/dev/null || ss -tlnp | grep -E ':443\b' || echo "(ничего на 443 — проверьте docker compose / firewall)"
else
  echo "(нет ss)"
fi

echo "== HTTPS по имени (как снаружи; на самом VPS часто падает: hairpin/NAT к своему публичному IP) =="
set +e
curl -sS -I --connect-timeout 15 "https://${PUNY:-$DOMAIN}/" 2>&1 | head -20
EC_OUT=$?
set -e
if [[ "$EC_OUT" -ne 0 ]]; then
  echo "(curl к публичному IP по имени завершился с ошибкой — см. ниже проверку через 127.0.0.1)"
fi

echo "== HTTPS на 127.0.0.1 с тем же SNI (обход hairpin; должно работать, если Caddy поднят) =="
CHK_HOST="${PUNY:-$DOMAIN}"
set +e
curl -sS -k -I --connect-timeout 10 "https://${CHK_HOST}/" \
  --resolve "${CHK_HOST}:443:127.0.0.1" 2>&1 | head -20
EC_LOC=$?
set -e
if [[ "$EC_LOC" -ne 0 ]]; then
  echo ":: на 127.0.0.1:443 тоже нет ответа — проверьте: docker compose ps, логи caddy, что порты 80/443 проброшены"
fi

echo "== Первые строки HTML (через 127.0.0.1 + --resolve) =="
set +e
curl -sS -k --connect-timeout 10 "https://${CHK_HOST}/" \
  --resolve "${CHK_HOST}:443:127.0.0.1" 2>/dev/null | tail -8
set -e

if command -v docker >/dev/null 2>&1 && [[ -f "$(pwd)/docker-compose.yml" || -f "./docker-compose.yml" ]]; then
  echo "== docker compose ps (текущий каталог) =="
  docker compose ps 2>/dev/null || true
fi

echo "== Готово =="
echo "Если DNS указывает на этот VPS, а curl к имени с сервера падает, а --resolve …:127.0.0.1 работает — это нормальный hairpin; с телефона/ПК сайт может открываться."
echo "В .env: CADDY_SITE_LABELS=${DOMAIN},${PUNY:-punycode_см_выше}"
