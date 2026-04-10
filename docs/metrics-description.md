# Metrics Description

## Аналитическая модель

Текущая модель опирается на две таблицы ClickHouse:

| Таблица | Источник | Ключевые поля |
| --- | --- | --- |
| `analytic.requested` | `RequestedEvent` | `event_date`, `cid`, `mid`, `count` |
| `analytic.refered` | `ReferedEvent` | `event_date`, `mid`, `count` |

`mid` соответствует `manufactureId`, а `cid` соответствует `categoryId`.

Важно: в систему не попадают сырые одиночные пользовательские события. `simulator` отправляет уже агрегированные значения `count` за тик. Поэтому все метрики ниже являются суммами и производными над этими пакетами.

## Реально рассчитываемые метрики

| Метрика | Где реализована | Формула по коду | Бизнес-смысл |
| --- | --- | --- | --- |
| `globalCount` | `AnalyticsClickHouseRepository.getTotalCount(...)` | `sum(count)` из `analytic.requested` по `mid` | сколько показов/попаданий в выдачу накоплено для `manufactureId` |
| `referCount` | `AnalyticsClickHouseRepository.getReferCount(...)` | `sum(count)` из `analytic.refered` по `mid` | сколько переходов на карточку накоплено для `manufactureId` |
| `averageRank` | `AnalyticsClickHouseRepository.getAverageRank(...)` | `row_number()` внутри `cid` по `count desc` + последующее усреднение | производная оценка ранга, связанная с категориями, в которых встречался `manufactureId` |

## `globalCount`

`globalCount` читается из таблицы `analytic.requested`:

- участвует только `RequestedEvent`;
- фильтр идет по `mid = manufactureId`;
- метрика не ограничена по времени;
- в ответе возвращается строкой.

Практический смысл: суммарный объем показов, который симулятор сгенерировал для указанного `manufactureId`.

## `referCount`

`referCount` читается из таблицы `analytic.refered`:

- участвует только `ReferedEvent`;
- фильтр идет по `mid = manufactureId`;
- метрика не ограничена по времени;
- в ответе возвращается строкой.

Практический смысл: суммарный объем переходов на карточку для указанного `manufactureId`.

## `averageRank`

`averageRank` считается сложнее:

1. из `analytic.requested` выбираются категории `cid`, где встречался нужный `mid`;
2. по всем строкам `analytic.requested` внутри каждой категории назначается `row_number() over (partition by cid order by count desc)`;
3. внутри каждой категории считается средний `rank`;
4. затем берется среднее по этим категориям.

## Ограничения интерпретации метрик

### Общие ограничения

- нет фильтрации по временному интервалу;
- нет уникальных пользователей, сессий, заказов и seller-level измерений;
- метрики пересчитываются on-demand и не материализуются в отдельной витрине;
- после перезапуска `simulator` набор `manufactureId` меняется, поэтому сравнивать запуски напрямую нельзя.

### Ограничения `averageRank`

`averageRank` требует особенно аккуратной формулировки:

- в запросе используется таблица `analytic.requested`, где нет отдельного поля ранга;
- ранг выводится косвенно через порядок строк по `count`;
- SQL не фильтрует ранжируемые строки по целевому `mid` после построения `row_number()`.

Из-за этого текущая реализация не дает точную «среднюю позицию конкретного товара в каталоге». Корректнее описывать ее как вычисляемую rank-based метрику по категориям, связанным с `manufactureId`.

## Какие сущности и события участвуют

- `RequestedEvent`
  - источник `globalCount`
  - единственный источник для `averageRank`
- `ReferedEvent`
  - источник `referCount`
- `AnalyticsService`
  - собирает итоговый DTO
- `AnalyticsClickHouseRepository`
  - содержит весь SQL-расчет метрик

## Примечание

В коде нет других аналитических метрик, кроме `globalCount`, `referCount` и `averageRank`. Упоминания дополнительных KPI или фильтров в старых текстах не подтверждаются текущей реализацией.
