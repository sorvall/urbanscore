#!/bin/sh
# Caddy: {$VAR:default} не подставляет default при пустой строке — контейнер падал с ошибкой парсинга.
# Если в .env задано CADDY_SITE_LABELS= (пусто), подставляем CADDY_DOMAIN или localhost.
set -eu
if [ -z "${CADDY_SITE_LABELS:-}" ]; then
	export CADDY_SITE_LABELS="${CADDY_DOMAIN:-localhost}"
fi
exec caddy run --config /etc/caddy/Caddyfile --adapter caddyfile
