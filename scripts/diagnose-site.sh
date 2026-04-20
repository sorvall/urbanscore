#!/usr/bin/env bash
# Запуск на сервере или локально: проверка DNS и ответа по HTTP(S).
# Пример: bash scripts/diagnose-site.sh мосдомэксперт.рф
set -euo pipefail

DOMAIN="${1:-мосдомэксперт.рф}"
PUNY="$(python3 -c "import sys; print(sys.argv[1].encode('idna').decode('ascii'))" "$DOMAIN" 2>/dev/null || true)"

echo "== Домен: $DOMAIN =="
echo "== DNS A (Google 8.8.8.8) =="
dig @8.8.8.8 +short "$DOMAIN" A || true
echo "== DNS AAAA =="
dig @8.8.8.8 +short "$DOMAIN" AAAA || true
if [[ -n "$PUNY" && "$PUNY" != "$DOMAIN" ]]; then
  echo "== Punycode для CADDY_SITE_LABELS (вторая часть после запятой) =="
  echo "$PUNY"
fi

echo "== HTTPS заголовки =="
curl -sS -I --connect-timeout 15 "https://${DOMAIN}/" | head -20 || echo "(ошибка curl)"

echo "== Первые строки HTML (ищем /assets/) =="
curl -sS --connect-timeout 15 "https://${DOMAIN}/" | tail -8 || true

echo "== Готово. Сравните A-запись с IP вашего VPS; в .env задайте CADDY_SITE_LABELS=${DOMAIN},${PUNY:-punycode_см_выше}"
