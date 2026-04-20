#!/usr/bin/env bash
# Первичная настройка VPS (Ubuntu/Debian): Docker + каталог деплоя.
# Запуск на сервере от root:  sudo bash scripts/server-bootstrap.sh
# Или после клонирования:     curl -fsSL ... | bash  (скопируйте файл на сервер вручную).
#
# Переменные окружения:
#   DEPLOY_PATH  — каталог для кода и .env (должен совпадать с секретом GitHub DEPLOY_PATH)
#                  по умолчанию: /root/urbanscore-app

set -euo pipefail

DEPLOY_PATH="${DEPLOY_PATH:-/root/urbanscore-app}"

echo "==> Urbanscore: установка Docker и каталога деплоя"
echo "    DEPLOY_PATH=$DEPLOY_PATH"

if [[ "${EUID:-$(id -u)}" -ne 0 ]]; then
  echo "Запустите от root или через sudo." >&2
  exit 1
fi

if command -v docker >/dev/null 2>&1; then
  echo "Docker уже установлен: $(docker --version)"
else
  echo "==> Устанавливаю Docker (get.docker.com)..."
  curl -fsSL https://get.docker.com | sh
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "::error::docker compose plugin не найден после установки Docker" >&2
  exit 1
fi

echo "==> Создаю каталог $DEPLOY_PATH"
mkdir -p "$DEPLOY_PATH"
chmod 700 "$DEPLOY_PATH" 2>/dev/null || true

ENV_FILE="$DEPLOY_PATH/.env"
if [[ ! -f "$ENV_FILE" ]]; then
  echo "==> Создаю пустой $ENV_FILE — заполните ключи (см. .env.example в репозитории)"
  umask 077
  cat >"$ENV_FILE" <<'EOF'
# Скопируйте из .env.example репозитория и подставьте значения.
DADATA_BASE_URL=https://suggestions.dadata.ru
DADATA_API_TOKEN=
DADATA_SECRET=
DADATA_ENABLED=true
DADATA_MIN_REQUEST_INTERVAL_MS=0
DADATA_ADDRESS_DIVISION=administrative

DEEPSEEK_API_KEY=

# Подставьте реальные публичные URL вашего сервера:
# VITE_API_BASE_URL — как браузер достучится до API (порт 8080 или прокси)
# VITE_SITE_URL — URL фронта (порт 5173 с хоста или домен за nginx)
VITE_API_BASE_URL=http://YOUR_HOST:8080
VITE_SITE_URL=http://YOUR_HOST:5173
APP_CORS_ORIGINS=http://YOUR_HOST:5173
EOF
  echo "Отредактируйте: nano $ENV_FILE"
else
  echo "Файл $ENV_FILE уже есть — не перезаписываю."
fi

echo
echo "Готово. Дальше:"
echo "  1. Заполните $ENV_FILE"
echo "  2. В GitHub → Settings → Secrets → Actions задайте SERVER_HOST, SERVER_USER, SSH_PASSWORD, DEPLOY_PATH"
echo "  3. Запустите workflow Deploy или сделайте push в main"
