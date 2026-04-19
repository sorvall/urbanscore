# Urbanscore

Клик по карте → адрес (DaData) → отчёт (DeepSeek).

## Переменные окружения

Обязательные для работы API:

| Переменная | Назначение |
|------------|------------|
| `DADATA_API_TOKEN` | Токен [DaData](https://dadata.ru/api/) (геокодирование) |
| `DEEPSEEK_API_KEY` | Ключ [DeepSeek](https://platform.deepseek.com/) |

Остальные (`DADATA_*`, `DEEPSEEK_*`, `APP_CORS_ORIGINS`, `SERVER_PORT`) имеют значения по умолчанию в `src/main/resources/application.yml`.

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
