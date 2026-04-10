# Deployment Guide

## Что используется для запуска

Основные файлы запуска:

- `docker-compose.yml`
- `.env`
- `modules/analytic/Dockerfile`
- `modules/simulator/Dockerfile`
- `modules/analytic-monitor/Dockerfile`
- `modules/analytic/src/main/resources/application.properties`
- `modules/simulator/src/main/resources/application.properties`

## Важный нюанс перед стартом

`analytic` и `simulator` не собираются внутри Dockerfile. Их Dockerfile просто копируют уже готовые JAR-файлы из `build/libs/`.

Поэтому фактический порядок запуска начинается с локальной сборки:

```bash
./gradlew build
```

После этого можно запускать Docker Compose.

## Рекомендуемый порядок запуска

### Безопасный по зависимостям вариант

1. Собрать backend-модули:

```bash
./gradlew build
```

2. Поднять инфраструктуру:

```bash
docker compose up -d clickhouse zookeeper kafka
```

3. Дождаться готовности Kafka и доступности ClickHouse по `http://localhost:8123`.

4. Поднять прикладные сервисы и UI:

```bash
docker compose up -d analytic simulator frontend kafka-ui ch-ui
```

### Полный запуск одной командой

```bash
docker compose up --build
```

Этот вариант имеет смысл только после `./gradlew build`.

## Почему безопасный порядок предпочтителен

- `analytic` зависит от Kafka через `depends_on`, но в compose не зависит от ClickHouse;
- при старте `analytic` сразу выполняет `CREATE DATABASE` и `CREATE TABLE` в ClickHouse;
- если ClickHouse еще не готов, backend может стартовать нестабильно.

## Состав сервисов

Команда `docker compose config --services` в текущем проекте возвращает:

- `zookeeper`
- `kafka`
- `analytic`
- `simulator`
- `frontend`
- `kafka-ui`
- `ch-ui`
- `clickhouse`

## Зависимости сервисов

| Сервис | Зависимости в compose | Фактические runtime-зависимости |
| --- | --- | --- |
| `zookeeper` | нет | нужен Kafka |
| `kafka` | `zookeeper` | нужен `analytic`, `simulator`, `kafka-ui` |
| `clickhouse` | нет | нужен `analytic`, косвенно нужен `ch-ui` |
| `analytic` | `kafka` (healthy) | Kafka и ClickHouse |
| `simulator` | `kafka` (healthy) | Kafka |
| `frontend` | `analytic`, `simulator` | по коду UI обращается к backend на `localhost:8001`; `simulator` полезен оператору для получения актуального `manufactureId`, но не вызывается самим frontend |
| `kafka-ui` | `kafka` (healthy) | Kafka |
| `ch-ui` | нет | ClickHouse по `localhost:8123` |

## Какие сервисы должны быть доступны после старта

| Сервис | URL / порт | Назначение |
| --- | --- | --- |
| Frontend monitor | `http://localhost:5173` | визуальный контроль метрик |
| Analytic API | `http://localhost:8001` | REST и WebSocket backend |
| Simulator API | `http://localhost:8002/simulator/config` | получение актуальных `manufactureId` и конфигурации |
| Kafka UI | `http://localhost:8080` | просмотр топиков |
| ClickHouse HTTP | `http://localhost:8123` | SQL-проверки |
| ClickHouse UI | `http://localhost:5521` | просмотр таблиц и запросов |
| Kafka broker | `localhost:9092` | внутренний и частично внешний доступ к Kafka |
| Zookeeper | `localhost:2181` | инфраструктурный сервис Kafka |

## Как проверить, что контур поднялся

### 1. Backend

Сначала получить актуальный `manufactureId`:

```bash
curl http://localhost:8002/simulator/config
```

Затем запросить аналитику:

```bash
curl -H "X-API-KEY: secret" http://localhost:8001/api/<manufactureId>/show
```

Ожидается JSON с полями `averageRank`, `globalCount`, `referCount`.

### 2. Kafka

- открыть `http://localhost:8080`
- убедиться, что есть топики `market-requested` и `market-referred`
- убедиться, что количество сообщений растет

### 3. ClickHouse

Проверка HTTP-запросом:

```bash
curl "http://localhost:8123/?query=SHOW%20TABLES%20FROM%20analytic"
```

Должны существовать таблицы `requested` и `refered`.

Проверка накопления данных:

```bash
curl "http://localhost:8123/?query=SELECT%20count()%20FROM%20analytic.requested"
```

### 4. Monitor

- открыть `http://localhost:5173`
- ввести `API Key`
- вставить `manufactureId`
- выбрать `REST polling` или `WebSocket`
- нажать `Start`

Признак работоспособности: меняются JSON-данные и график.

## Healthcheck

Явный healthcheck в compose задан только для `kafka`.

В проекте не найдены:

- `Spring Actuator`
- HTTP health endpoint для `analytic`
- HTTP health endpoint для `simulator`
- отдельные healthcheck'и для `clickhouse`, `frontend`, `kafka-ui`, `ch-ui`

## Режимы запуска и работы

### Режимы запуска

- один compose-контур без profiles;
- безопасный staged startup;
- полный запуск одной командой после предварительной сборки JAR.

### Режимы работы монитора

- `REST polling`: периодический `GET /api/{manufactureId}/show`
- `WebSocket`: push-обновления по `/ws/analytics`

## Ограничения и примечания

- `.env` присутствует, но текущий `docker-compose.yml` не пробрасывает `API_KEY`, `CLICKHOUSE_*` и другие значения в контейнер `analytic`. Поэтому сервис использует дефолты из `application.properties`, если переменные не экспортированы отдельно.
- Frontend использует жестко заданный backend URL `http://localhost:8001/api` и `ws://localhost:8001/ws/analytics`.
- В `docker-compose.yml` у Kafka объявлен listener `PLAINTEXT_HOST://localhost:29092`, но порт `29092` не опубликован. Собственные сервисы проекта работают через `kafka:9092`; внешний доступ к Kafka с хоста в текущем compose описан не полностью.
