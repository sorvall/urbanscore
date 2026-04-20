#!/bin/sh
# Caddy: {$VAR:default} не подставляет default при пустой строке — контейнер падал с ошибкой парсинга.
# Если в .env задано CADDY_SITE_LABELS= (пусто), подставляем CADDY_DOMAIN или localhost.
set -eu
if [ -z "${CADDY_SITE_LABELS:-}" ]; then
	export CADDY_SITE_LABELS="${CADDY_DOMAIN:-localhost}"
fi
# Caddy 2.8+: в списке site-адресов после запятой нужен пробел: «a, b», иначе ошибка adapting config.
export CADDY_SITE_LABELS="$(printf '%s' "$CADDY_SITE_LABELS" | sed 's/, */, /g')"
exec caddy run --config /etc/caddy/Caddyfile --adapter caddyfile
