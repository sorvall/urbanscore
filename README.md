# Urbanscore

Клик по карте → адрес (DaData) → отчёт (DeepSeek). **База данных и Redis не используются** — только HTTP API и внешние сервисы.

## Переменные окружения

Обязательные для работы API:

| Переменная | Назначение |
|------------|------------|
| `DADATA_API_TOKEN` | Токен [DaData](https://dadata.ru/api/) (геокодирование) |
| `DEEPSEEK_API_KEY` | Ключ [DeepSeek](https://platform.deepseek.com/) |

Остальные (`DADATA_*`, `DEEPSEEK_*`, `APP_CORS_ORIGINS`, `SERVER_PORT`) имеют значения по умолчанию в `src/main/resources/application.yml`.

Системный промпт DeepSeek лежит в **`src/main/resources/prompts/deepseek-system-prompt.txt`** — правьте файл и перезапускайте приложение. Целиком из переменной окружения: `DEEPSEEK_SYSTEM_PROMPT`; другой файл: `DEEPSEEK_SYSTEM_PROMPT_PATH` (например `file:/path/to/prompt.txt`).

API: `GET /api/v1/geocode?q=…` — геокодирование строки адреса (DaData suggest), ответ `{ address, lat, lon }` для карты и отчёта.

Скопируйте `.env.example` в `.env` и заполните ключи. Файл `.env` в репозиторий не коммитится.

## Запуск (Docker)

```bash
docker compose up -d --build
```

- Фронт: http://localhost:5173  
- API: http://localhost:8080  

Фронт при сборке получает `VITE_API_BASE_URL=http://localhost:8080` (см. `docker-compose.yml`).

## Локальная разработка

- Бэкенд: `mvn spring-boot:run` (при необходимости положите `.env` в корень — `UrbanScoreApplication` подхватывает его для локального запуска).
- Фронт: `cd frontend && npm install && npm run dev`, переменная `VITE_API_BASE_URL` по умолчанию `http://localhost:8080`.
