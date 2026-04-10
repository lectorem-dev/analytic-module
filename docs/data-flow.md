# Data Flow

## Источник событий

События не приходят из внешней БД и не загружаются вручную через REST. В текущем коде они создаются внутри `modules/simulator`:

- `GeneratorConfigInitializer` один раз генерирует набор `categoryId` и `manufactureId`;
- `SimulationStateService` рассчитывает активные категории, ранжирование и ожидаемый трафик;
- `EventGeneratorService` по расписанию `simulator.generate-interval-ms=1200` мс публикует события.

Используются два типа событий:

- `RequestedEvent(categoryId, manufactureId, count, eventDate)`
- `ReferedEvent(manufactureId, count, eventDate)`

Поле `count` означает уже агрегированное количество действий за тик симуляции, а не одно пользовательское действие на одно сообщение.

## Пошаговый поток данных

1. `SimulationStateService.nextTick()` формирует план трафика по активным категориям.
2. `EventGeneratorService.generateEvents()` превращает этот план в набор `RequestedEvent` и `ReferedEvent`.
3. `KafkaEventPublisher` отправляет события в Kafka:
   - `RequestedEvent` -> топик `market-requested`, ключ сообщения `categoryId`
   - `ReferedEvent` -> топик `market-referred`, ключ сообщения `manufactureId`
4. `AnalyticsKafkaListener` в сервисе `analytic` получает сообщения из обоих топиков.
5. Listener десериализует payload через `ObjectMapper.convertValue(...)` в модели из `libs-analytic`.
6. `LoadDataService` передает данные в `DataLoadClickHouseRepository`.
7. `DataLoadClickHouseRepository` вставляет строки в ClickHouse:
   - `analytic.requested(event_date, cid, mid, count)`
   - `analytic.refered(event_date, mid, count)`
8. После вставки listener вызывает `AnalyticsWebSocketPublisher.scheduleSnapshot(event.getManufactureId())`.
9. При REST-запросе `AnalyticsController` вызывает `AnalyticsService`, а тот читает агрегаты через `AnalyticsClickHouseRepository`.
10. При WebSocket-подписке `AnalyticsWebSocketPublisher` через 300 мс формирует снапшот `getAnalyticShowDTO(...)` и отправляет его через `AnalyticsWebSocketHandler`.

## Как backend читает и обрабатывает данные

### Kafka -> backend

- Consumer подписан на `market-requested` и `market-referred`.
- Группа consumer'а: `analytics-service`.
- В listener нет дополнительной бизнес-валидации кроме попытки преобразовать `Map<String, Object>` в целевую модель.
- Ошибки обработки только логируются; отдельного retry-контура или dead-letter topic в коде нет.

### Backend -> ClickHouse

- `ClickHouseConfig` на старте создает БД `analytic` и таблицы `requested` / `refered`.
- Запись выполняется простыми `INSERT` через `JdbcTemplate`.
- Дополнительной дедупликации или промежуточной витрины нет: каждое принятое сообщение сразу пишется как новая строка.

### ClickHouse -> ответ

- `globalCount` считается как `sum(count)` по `analytic.requested` для `mid`.
- `referCount` считается как `sum(count)` по `analytic.refered` для `mid`.
- `averageRank` считается SQL-запросом с `row_number() over (partition by cid order by count desc)` и дальнейшим усреднением.
- `AnalyticsService` преобразует числовые значения в строки и собирает `AnalyticsResponse`.

## Разделение REST и WebSocket

| Канал | Когда формируется ответ | Что возвращается | Ограничение |
| --- | --- | --- | --- |
| REST | Только по входящему HTTP-запросу | `AnalyticsResponse` или его подмножество | каждый запрос заново читает ClickHouse |
| WebSocket | Только после новых Kafka-событий и только при наличии подписчиков | `AnalyticsWsMessage` c полным `AnalyticsResponse` | при подключении нет стартового снапшота, приходят только новые обновления |

REST использует четыре отдельных endpoint'а, а WebSocket всегда публикует полный снапшот, эквивалентный `GET /api/{manufactureId}/show`.

## Сквозной сценарий

1. Пользователь или разработчик получает актуальный `manufactureId` через `GET /simulator/config`.
2. `simulator` начинает генерировать тики и отправлять сообщения в Kafka.
3. `analytic` принимает события и сохраняет их в ClickHouse.
4. Для проверки по REST клиент вызывает `GET /api/{manufactureId}/show` с `X-API-KEY`.
5. Для проверки по WebSocket клиент подключается к `/ws/analytics?manufactureId=...&apiKey=...`.
6. После следующего сохраненного события для этого `manufactureId` backend публикует сообщение `analytics.snapshot`.
7. `analytic-monitor` показывает текущий JSON-ответ и добавляет точку в график истории.

## Ограничения

- `manufactureId` и `categoryId` создаются заново при каждом старте `simulator`, поэтому значения не стабильны между перезапусками.
- В текущем API нет фильтрации по времени, категории, seller или session.
- WebSocket не умеет отправлять историю и не выполняет начальную загрузку состояния на connect.
- Поток данных событийный, но сами сообщения уже содержат агрегированное поле `count`, а не сырые одиночные события.
