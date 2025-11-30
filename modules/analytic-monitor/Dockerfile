# -------------------------------------------------------
# 1. Stage: Build frontend (Vite)
# -------------------------------------------------------
FROM node:18-alpine AS build

WORKDIR /app

# Оптимизация слоёв
COPY package.json package-lock.json* pnpm-lock.yaml* yarn.lock* ./
RUN npm install

COPY . .
RUN npm run build


# -------------------------------------------------------
# 2. Stage: Production server (nginx)
# -------------------------------------------------------
FROM nginx:alpine

# Удаляем дефолтную конфигурацию
RUN rm -rf /usr/share/nginx/html/*

# Копируем билд фронта
COPY --from=build /app/dist /usr/share/nginx/html

# Кастомная nginx-конфигурация (SPA routing)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 5173

CMD ["nginx", "-g", "daemon off;"]