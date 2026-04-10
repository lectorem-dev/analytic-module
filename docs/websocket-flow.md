# WebSocket Flow

## Наличие WebSocket

WebSocket в проекте есть и является актуальной частью backend-а. Он реализован через raw Spring WebSocket без STOMP и SockJS.

## Endpoint подключения

- path: `/ws/analytics`
- регистрация: `AnalyticsWebSocketConfig.registerWebSocketHandlers(...)`
- allowed origin: `http://localhost:5173`

## Как клиент подключается

Подключение проходит через handshake interceptor `AnalyticsHandshakeInterceptor`.

Обязательные данные:

- `manufactureId` в query string
- API key:
  - либо заголовком `X-API-KEY`
  - либо query-параметром `apiKey`

React-монитор использует второй вариант и открывает соединение вида:

```text
ws://localhost:8001/ws/analytics?manufactureId=<UUID>&apiKey=<API_KEY>
```

Если ключ неверный, сервер отклоняет handshake с `403`. Если `manufactureId` отсутствует или не является UUID, handshake завершается с `400`.

## Что публикуется

Сервер отправляет сообщение типа `AnalyticsWsMessage`:

```json
{
  "type": "analytics.snapshot",
  "manufactureId": "d4f7f66a-8d55-4b2b-9ed6-5f0cc6f9ec15",
  "generatedAt": "2026-04-10T08:15:30.123Z",
  "analytics": {
    "averageRank": "12.5",
    "globalCount": "840",
    "referCount": "97"
  }
}
```

Поле `analytics` всегда содержит полный снапшот, эквивалентный `GET /api/{manufactureId}/show`.

## Когда отправляются обновления

1. `AnalyticsKafkaListener` получает новое событие из `market-requested` или `market-referred`.
2. После успешной записи в ClickHouse listener вызывает `AnalyticsWebSocketPublisher.scheduleSnapshot(manufactureId)`.
3. Публикация откладывается на `analytics.ws.publish-delay-ms=300` мс.
4. Если за это время приходят новые события для того же `manufactureId`, предыдущий отложенный publish отменяется и заменяется новым.
5. `AnalyticsWebSocketHandler.broadcast(...)` рассылает сериализованное сообщение всем активным подписчикам этого `manufactureId`.

Таким образом WebSocket работает как debounce-push поверх Kafka ingest.

## Backend-компоненты

- `AnalyticsWebSocketConfig`: регистрирует endpoint и scheduler.
- `AnalyticsHandshakeInterceptor`: аутентификация и извлечение `manufactureId`.
- `AnalyticsWebSocketHandler`: хранение сессий по `manufactureId` и отправка сообщений.
- `AnalyticsWebSocketPublisher`: отложенное создание снапшотов.
- `AnalyticsService`: собирает `AnalyticsResponse`, который попадает в WebSocket payload.
- `AnalyticsKafkaListener`: триггерит публикацию после новой записи.

## Ограничения сценария

- Сервер не отправляет начальный снапшот в момент подключения. Клиент получает только новые обновления после connect.
- Подписка привязана к одному `manufactureId`, переданному на handshake. Изменить его без нового соединения нельзя.
- Клиентские входящие сообщения backend не обрабатывает; соединение используется только для server push.
- Публикация происходит только если на момент события у `manufactureId` есть активные подписчики.
- В коде нет replay, history, подтверждений доставки и отдельного heartbeat-протокола на уровне приложения.
