# ETL And Processing

## Текущая модель обработки

В актуальной версии проекта отдельного ETL-процесса из PostgreSQL или другой внешней БД нет. Текущая линия обработки является событийной:

`simulator -> Kafka -> analytic -> ClickHouse -> SQL aggregation -> REST / WebSocket`

Это ingestion-пайплайн реального времени, а не batch ETL.

## Ранняя и актуальная версия

### Ранняя версия

По истории репозитория и старым документам проект раньше использовал прямую загрузку событий через REST и `WebFilter` для API key.

### Актуальная версия

- данные создаются встроенным симулятором;
- ingestion идет через Kafka;
- хранение реализовано только в ClickHouse;
- расчет метрик выполняется в момент чтения, без materialized aggregate table.

## Этапы обработки данных

| Этап | Где реализован | Что происходит |
| --- | --- | --- |
| Генерация идентификаторов и справочников | `GeneratorConfigInitializer` | создаются случайные `categoryId`, `manufactureId`, карты популярности и спроса |
| Моделирование трафика | `SimulationStateService` | рассчитываются активные категории, ранжирование, количество показов и переходов |
| Публикация событий | `EventGeneratorService`, `KafkaEventPublisher` | формируются `RequestedEvent` и `ReferedEvent` и отправляются в Kafka |
| Прием и нормализация | `AnalyticsKafkaListener` | `Map<String, Object>` преобразуется в целевые модели `RequestedEvent` / `ReferedEvent` |
| Запись в хранилище | `LoadDataService`, `DataLoadClickHouseRepository` | поля события маппятся на колонки ClickHouse и вставляются через `JdbcTemplate` |
| Построение ответа | `AnalyticsService`, `AnalyticsClickHouseRepository` | выполняются SQL-запросы `sum(...)` и `avg(...)`, затем создается `AnalyticsResponse` |
| Push-обновление | `AnalyticsWebSocketPublisher`, `AnalyticsWebSocketHandler` | после новой записи публикуется отложенный снапшот для подписчиков |

## Где происходят преобразования данных

### В `simulator`

- `SimulationStateService` распределяет общий трафик категории по ранжированному списку товаров.
- `estimateClicks(...)` пересчитывает показы в ожидаемые переходы.
- `EventGeneratorService` сворачивает модель тика в два типа событий с датой `LocalDate.now()`.

### В `analytic`

- `AnalyticsKafkaListener` преобразует входящее сообщение из generic-структуры в доменную модель через `ObjectMapper`.
- `DataLoadClickHouseRepository` приводит `LocalDate` к `java.sql.Date`, а UUID к строкам перед записью.
- `AnalyticsService` приводит числовые результаты SQL к строковым полям DTO.

### В ClickHouse

- физическая схема создается на старте приложения, а не миграциями;
- агрегирование выполняется SQL-запросами по таблицам `analytic.requested` и `analytic.refered`;
- отдельной витрины, materialized view или background job нет.

## Ключевые классы и методы

- `EventGeneratorService.generateEvents()`
- `SimulationStateService.nextTick()`
- `KafkaEventPublisher.sendRequested(...)`
- `KafkaEventPublisher.sendReferred(...)`
- `AnalyticsKafkaListener.onRequestedEvent(...)`
- `AnalyticsKafkaListener.onReferredEvent(...)`
- `LoadDataService.loadRequested(...)`
- `LoadDataService.loadReferred(...)`
- `DataLoadClickHouseRepository.loadRequested(...)`
- `DataLoadClickHouseRepository.loadReferred(...)`
- `AnalyticsClickHouseRepository.getAverageRank(...)`
- `AnalyticsClickHouseRepository.getTotalCount(...)`
- `AnalyticsClickHouseRepository.getReferCount(...)`
- `AnalyticsWebSocketPublisher.scheduleSnapshot(...)`

## Ограничения

- В текущем коде нет PostgreSQL, JDBC-выгрузки из внешней OLTP-БД, batch job или расписания ETL.
- Вставка в ClickHouse не содержит дедупликации: повторно обработанное Kafka-сообщение даст повторную строку.
- В проекте есть две стратегии создания Kafka-топиков:
  - `KafkaTopicConfig` в `simulator` создает `market-requested` и `market-referred` с `3` партициями;
  - `KafkaTopicChecker` в `analytic` может создать отсутствующие топики с `1` партицией.
  Фактическое число партиций зависит от того, какой сервис создаст топик первым.
