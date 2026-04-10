# Architecture Overview

## Назначение модуля

Аналитический модуль в текущей реализации принимает синтетические события каталога и переходов из `simulator`, сохраняет их в ClickHouse, рассчитывает три метрики (`globalCount`, `referCount`, `averageRank`) и отдает результат через REST API и WebSocket.

Текущий контур предназначен не для интеграции с внешним marketplace, а для демонстрации полного pipeline внутри локального стенда.

## Место в инфраструктуре проекта

В инфраструктуре репозитория модуль занимает центральное место между генератором событий и пользовательской панелью:

`simulator -> Kafka -> analytic -> ClickHouse -> REST / WebSocket -> analytic-monitor`

Сервис `analytic` является основным backend-компонентом. Остальные сервисы обслуживают источник данных, визуализацию и инфраструктурное окружение.

## Основные компоненты системы

| Компонент | Роль в текущей архитектуре | Подтверждающие артефакты |
| --- | --- | --- |
| `modules/analytic` | Spring Boot backend аналитики: Kafka consumer, ClickHouse access, REST API, WebSocket | `AnalyticsKafkaListener`, `AnalyticsController`, `AnalyticsClickHouseRepository`, `AnalyticsWebSocketHandler` |
| `modules/simulator` | Генерация синтетических событий по расписанию | `EventGeneratorService`, `SimulationStateService`, `SimulatorConfigController` |
| `modules/libs-analytic` | Общие модели Kafka-сообщений | `RequestedEvent`, `ReferedEvent` |
| `modules/analytic-monitor` | React-панель мониторинга с режимами `REST polling` и `WebSocket` | `MonitorPanel.jsx` |
| `kafka` + `zookeeper` | Транспорт событий между `simulator` и `analytic` | `docker-compose.yml`, Kafka-конфиги модулей |
| `clickhouse` | Хранение сырых событий и SQL-агрегация метрик | `ClickHouseConfig`, ClickHouse repositories |
| `kafka-ui` | Просмотр топиков и сообщений Kafka | `docker-compose.yml` |
| `ch-ui` | Просмотр данных ClickHouse | `docker-compose.yml` |

## Связи между компонентами

1. `GeneratorConfigInitializer` при старте `simulator` создает случайные `categoryId` и `manufactureId`.
2. `SimulationStateService` на каждом тике формирует план трафика по категориям и товарам.
3. `EventGeneratorService` публикует `RequestedEvent` и `ReferedEvent` через `KafkaEventPublisher`.
4. `AnalyticsKafkaListener` читает топики `market-requested` и `market-referred`.
5. `LoadDataService` делегирует запись в `DataLoadClickHouseRepository`.
6. `ClickHouseConfig` создает БД `analytic` и таблицы `analytic.requested` и `analytic.refered`.
7. `AnalyticsController` и `AnalyticsWebSocketPublisher` используют `AnalyticsService`, который читает метрики через `AnalyticsClickHouseRepository`.
8. `analytic-monitor` получает данные либо запросом `GET /api/{manufactureId}/show`, либо через `/ws/analytics`.

## Актуальная архитектурная цепочка

### Актуальная версия

- ingestion событий идет через Kafka;
- источник данных встроен в `simulator`;
- хранение сырых данных реализовано только в ClickHouse;
- расчет метрик выполняется on-demand SQL-запросами;
- доставка в UI поддерживает два канала: REST и WebSocket.

### Ранняя версия

По истории репозитория и старым документам в проекте раньше была линия с прямой REST-загрузкой событий и `WebFilter` для API key. Эта линия не соответствует текущему коду `modules/analytic` и не используется в актуальной архитектуре.

## Ключевые классы и сервисы

- `AnalyticsController`: публичный REST API `/api/{manufactureId}/...`.
- `AnalyticsService`: собирает `AnalyticsResponse` из трех метрик.
- `AnalyticsKafkaListener`: точка входа Kafka-событий в backend.
- `DataLoadClickHouseRepository`: вставка входящих событий в ClickHouse.
- `AnalyticsClickHouseRepository`: SQL-расчет метрик.
- `ClickHouseConfig`: datasource и автосоздание схемы ClickHouse.
- `AnalyticsWebSocketPublisher`: отложенная публикация снапшотов после новых событий.
- `AnalyticsHandshakeInterceptor`: проверка `apiKey` и `manufactureId` на WebSocket handshake.
- `AnalyticsWebSocketHandler`: хранение подписчиков и рассылка сообщений.
- `EventGeneratorService`: плановая генерация сообщений в `simulator`.
- `SimulationStateService`: модель трафика, трендов и промо-сценариев.
- `MonitorPanel`: основной UI-компонент панели мониторинга.

## Внешние зависимости модуля

- Kafka и Zookeeper из `docker-compose.yml`.
- ClickHouse и `ch-ui`.
- `provectuslabs/kafka-ui`.
- Spring Boot `web`, `websocket`, `jdbc`, `spring-kafka`.
- `com.clickhouse:clickhouse-jdbc`.
- `org.springdoc:springdoc-openapi-starter-webmvc-ui`.
- React, Axios, Chart.js, Nginx в `analytic-monitor`.

## Примечание

- Пакетная структура backend использует элементы `ports/adapters` (`application/in`, `application/out`, `adapters/in`, `adapters/out`), но текущая реализация не является строго изолированной hexagonal-архитектурой: `AnalyticsService` возвращает DTO из adapter-слоя.
- В коде аналитический ключ называется `manufactureId` / `mid`. В UI и старых текстах он местами описан как UUID товара; в документации ниже используется фактическое имя из кода.
