#!/usr/bin/env bash
# Интерактивно записывает секреты деплоя в GitHub (нужен gh CLI: brew install gh && gh auth login).
# Запуск из корня репозитория:  bash scripts/gh-set-deploy-secrets.sh
#
# Значения не сохраняются в файлах — только в GitHub Secrets.

set -euo pipefail

if ! command -v gh >/dev/null 2>&1; then
  echo "Установите GitHub CLI: https://cli.github.com/" >&2
  exit 1
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "Выполните: gh auth login" >&2
  exit 1
fi

read_secret() {
  local prompt="$1"
  local var
  read -r -s -p "$prompt" var
  echo
  printf '%s' "$var"
}

echo "Введите секреты для Actions (пустой ввод — пропустить существующее, кроме обязательных)."
echo

read -r -p "SERVER_HOST (IP или домен VPS): " SERVER_HOST
read -r -p "SERVER_USER (например root): " SERVER_USER
read -r -p "DEPLOY_PATH (например /root/urbanscore-app): " DEPLOY_PATH
read -r -p "SSH_PORT [22]: " SSH_PORT
SSH_PORT="${SSH_PORT:-22}"

if [[ -z "${SERVER_HOST}" || -z "${SERVER_USER}" || -z "${DEPLOY_PATH}" ]]; then
  echo "SERVER_HOST, SERVER_USER и DEPLOY_PATH обязательны." >&2
  exit 1
fi

echo
PW="$(read_secret "SSH_PASSWORD: ")"
if [[ -z "${PW}" ]]; then
  echo "SSH_PASSWORD не может быть пустым." >&2
  exit 1
fi

gh secret set SERVER_HOST -b"${SERVER_HOST}"
gh secret set SERVER_USER -b"${SERVER_USER}"
gh secret set DEPLOY_PATH -b"${DEPLOY_PATH}"
gh secret set SSH_PASSWORD -b"${PW}"
gh secret set SSH_PORT -b"${SSH_PORT}"

echo
echo "Секреты записаны. Проверка: gh secret list"
