FROM node:22-alpine AS build
WORKDIR /app

ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ARG VITE_SITE_URL=http://localhost:5173
ENV VITE_SITE_URL=$VITE_SITE_URL
ARG VITE_YM_ID=
ENV VITE_YM_ID=$VITE_YM_ID

COPY package.json ./
RUN npm install
COPY . .
# На сервере нет секретов GitHub: при деплое здесь пересобирали фронт без VITE_YM_ID и затирали dist из CI.
# Если VITE_YM_ID не задан в build-arg и уже есть dist из архива — не пересобираем (счётчик остаётся из CI).
# Если VITE_YM_ID задан в .env на сервере — всегда пересобираем.
RUN if [ -f dist/index.html ] && [ -z "${VITE_YM_ID:-}" ]; then \
      echo "frontend: using existing dist (from CI tarball)"; \
    else \
      npm run build; \
    fi

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
