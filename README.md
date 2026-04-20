# Urbanscore

Клик по карте → адрес (DaData) → отчёт (DeepSeek). **База данных и Redis не используются** — только HTTP API и внешние сервисы.

## Переменные окружения

Обязательные для работы API:

| Переменная | Назначение |
|------------|------------|
| `DADATA_API_TOKEN` | Токен [DaData](https://dadata.ru/api/) (геокодирование) |
| `DEEPSEEK_API_KEY` | Ключ [DeepSeek](https://platform.deepseek.com/) |

Остальные (`DADATA_*`, `DEEPSEEK_*`, `APP_CORS_ORIGINS`, `SERVER_PORT`) имеют значения по умолчанию в `src/main/resources/application.yml`.

- **Системный** промпт (формат ответа): **`src/main/resources/prompts/deepseek-system-prompt.txt`**. Подмена: `DEEPSEEK_SYSTEM_PROMPT` или `DEEPSEEK_SYSTEM_PROMPT_PATH`.
- **Пользовательское задание** (текст экспертного отчёта): **`src/main/resources/prompts/deepseek-user-prompt.txt`**. Подмена: `DEEPSEEK_USER_PROMPT` или `DEEPSEEK_USER_PROMPT_PATH`.
- Запросы к DeepSeek с **веб-поиском**: в теле передаётся **`search_enable: true`** (не `enable_search`). Отключить: **`DEEPSEEK_ENABLE_SEARCH=false`**.

Фронтенд в `POST /api/v1/report` передаёт только **`address`**; полный промпт подставляет бэкенд.

API: `GET /api/v1/geocode?q=…` — геокодирование строки адреса (DaData suggest), ответ `{ address, lat, lon }` для карты и отчёта.

Скопируйте `.env.example` в `.env` и заполните ключи. Файл `.env` в репозиторий не коммитится.

## Запуск (Docker)

```bash
docker compose up -d --build
```

- Фронт: http://localhost:5173  
- API: http://localhost:8080  

Если **`VITE_API_BASE_URL` пустой** (один домен за Caddy), nginx во фронтенде проксирует **`/api/*` на контейнер `app`** (`frontend/nginx.conf`) — запросы с карты и формы идут на тот же хост и порт, что и страница (в т.ч. при открытии по `http://IP:5173`).

Фронт при сборке получает `VITE_API_BASE_URL=http://localhost:8080` (см. `docker-compose.yml`). Для SEO (canonical, `robots.txt`, `sitemap.xml`) задайте **`VITE_SITE_URL`** — боевой URL без завершающего слэша, например `https://urbanscore.example.ru` (в `docker-compose.yml` передаётся как build-arg; для локальной сборки см. `frontend/.env.example`).

### HTTPS (Let's Encrypt)

В `docker-compose.yml` поднят **Caddy**: он запрашивает и обновляет сертификаты у Let's Encrypt для домена из **`CADDY_DOMAIN`**, проксирует `/api/*` на бэкенд и остальное на фронт. Нужны **открытые порты 80 и 443** на сервере и **A-запись DNS** домена на IP сервера.

На сервере в `.env` (в каталоге деплоя), затем `docker compose up -d --build`:

- **`CADDY_DOMAIN`** — ваш домен, например `мосдомэксперт.рф` (без `https://`). Почту для ACME при желании добавьте в начало `Caddyfile`: глобальный блок `{ email you@example.com }` (см. [документацию Caddy](https://caddyserver.com/docs/caddyfile/options#email)).
- **`VITE_API_BASE_URL=`** — пусто, чтобы фронт ходил в API по тому же хосту через прокси.
- **`VITE_SITE_URL=https://мосдомэксперт.рф`** (ваш URL).
- **`APP_CORS_ORIGINS`** — по умолчанию **`*`** (любой Origin): и домен, и `http://IP:5173` работают. Чтобы сузить список, укажите через запятую, например `https://мосдомэксперт.рф,http://72.56.232.145:5173`.

Локально без публичного домена по умолчанию используется `CADDY_DOMAIN=localhost` (без выдачи LE). Прямой доступ по-прежнему: порты **5173** (фронт) и **8080** (API).

**Яндекс:** добавьте сайт в [Яндекс.Вебмастер](https://webmaster.yandex.ru/), укажите `sitemap.xml` из корня сайта, при необходимости вставьте в `frontend/index.html` мета-тег подтверждения из кабинета (`<meta name="yandex-verification" content="…" />`) и пересоберите фронт.

## Локальная разработка

- Бэкенд: `mvn spring-boot:run` (при необходимости положите `.env` в корень — `UrbanScoreApplication` подхватывает его для локального запуска).
- Фронт: `cd frontend && npm install && npm run dev`, переменная `VITE_API_BASE_URL` по умолчанию `http://localhost:8080`.
