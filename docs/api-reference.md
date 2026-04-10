# API Reference

## Общие правила

- Базовый префикс аналитического API: `/api`
- Контроллер: `AnalyticsController`
- Все актуальные endpoint'ы являются `GET`
- Аналитический идентификатор передается как path-параметр `manufactureId`
- Доступ контролируется заголовком `X-API-KEY`

Если `X-API-KEY` отсутствует или не совпадает со значением `api.key`, контроллер возвращает `403 Forbidden` без тела.

## Актуальные REST endpoint'ы

| Метод | Путь | Параметры | Заголовки | Ответ |
| --- | --- | --- | --- | --- |
| `GET` | `/api/{manufactureId}/show` | `manufactureId: UUID` | `X-API-KEY` | полный `AnalyticsResponse` |
| `GET` | `/api/{manufactureId}/avg` | `manufactureId: UUID` | `X-API-KEY` | только `averageRank` |
| `GET` | `/api/{manufactureId}/refer` | `manufactureId: UUID` | `X-API-KEY` | только `referCount` |
| `GET` | `/api/{manufactureId}/count` | `manufactureId: UUID` | `X-API-KEY` | только `globalCount` |

В контроллере нет query-параметров, фильтров по времени и фильтров по категориям.

## DTO ответа

`AnalyticsResponse` имеет три строковых поля:

| Поле | Тип в JSON | Когда заполняется |
| --- | --- | --- |
| `averageRank` | `string` | `/show`, `/avg` |
| `globalCount` | `string` | `/show`, `/count` |
| `referCount` | `string` | `/show`, `/refer` |

Числа сериализуются как строки, потому что `AnalyticsService` вызывает `toString()` на результатах SQL.

## Примеры ответов

### `GET /api/{manufactureId}/show`

```json
{
  "averageRank": "12.5",
  "globalCount": "840",
  "referCount": "97"
}
```

### `GET /api/{manufactureId}/avg`

```json
{
  "averageRank": "12.5",
  "globalCount": null,
  "referCount": null
}
```

### `GET /api/{manufactureId}/refer`

```json
{
  "averageRank": null,
  "globalCount": null,
  "referCount": "97"
}
```

### `GET /api/{manufactureId}/count`

```json
{
  "averageRank": null,
  "globalCount": "840",
  "referCount": null
}
```

## Требования к API key

- REST-контроллер читает ключ из заголовка `X-API-KEY`
- Значение сравнивается вручную методом `isValidApiKey(...)`
- Значение ключа берется из `api.key=${API_KEY:secret}`

Практический нюанс для текущего Docker Compose: файл `.env` содержит `API_KEY=my-secret-key`, но `docker-compose.yml` не передает эту переменную в контейнер `analytic`. Поэтому при обычном запуске через compose backend использует значение по умолчанию `secret`, если переменная окружения не экспортирована отдельно.

## WebSocket

WebSocket-канал существует и используется монитором, но это отдельный транспорт, а не часть REST API. Кратко:

- endpoint: `/ws/analytics`
- обязательный query-параметр: `manufactureId`
- API key можно передать заголовком `X-API-KEY` или query-параметром `apiKey`

Подробности вынесены в [websocket-flow.md](websocket-flow.md).

## Примечание

- В текущем коде нет endpoint'ов для загрузки событий в `analytic` по REST. Старые упоминания `POST /api/load/...` относятся к ранней версии проекта.
- В `build.gradle` подключен `springdoc-openapi-starter-webmvc-ui`, а в контроллере есть `@Operation`, но явный URL Swagger UI в проектной конфигурации не задан.
