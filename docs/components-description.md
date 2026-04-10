# Components Description

## Controller

| Компонент | Назначение |
| --- | --- |
| `AnalyticsController` | Основной HTTP-вход в аналитический backend. Отдает четыре `GET`-endpoint'а и вручную проверяет `X-API-KEY`. |
| `SimulatorConfigController` | Вспомогательный REST-интерфейс `simulator`: отдает текущую конфигурацию и состояние генератора, позволяет переключать сценарий и запускать promo. |

## Service / Use Case

| Компонент | Назначение |
| --- | --- |
| `AnalyticsService` | Сервис чтения аналитики. Собирает `AnalyticsResponse` из трех метрик ClickHouse. |
| `LoadDataService` | Сервис записи входящих событий. Делегирует сохранение в `LoadDataPort`. |
| `AnalyticsWebSocketPublisher` | Публикует отложенные WebSocket-снапшоты после новых событий Kafka. |
| `EventGeneratorService` | Плановый генератор событий в `simulator`. Создает `RequestedEvent` и `ReferedEvent` на каждом тике. |
| `SimulationStateService` | Основная модель симуляции: сценарии трафика, тренды, промо, ранжирование и расчет `requestedCount` / `referredCount`. |

## Kafka

| Компонент | Назначение |
| --- | --- |
| `AnalyticsKafkaListener` | Принимает `market-requested` и `market-referred`, десериализует их и передает в сохранение. |
| `KafkaEventPublisher` | Публикует события симулятора в Kafka. |
| `KafkaTopicChecker` | В `analytic` ждет появления топиков и при необходимости создает их. |
| `KafkaTopicConfig` | В `simulator` объявляет `NewTopic` для `market-requested` и `market-referred`. |

## ClickHouse / Repository

| Компонент | Назначение |
| --- | --- |
| `ClickHouseConfig` | Создает datasource, `JdbcTemplate`, БД `analytic` и таблицы `requested` / `refered`. |
| `DataLoadClickHouseRepository` | Выполняет `INSERT` входящих событий в ClickHouse. |
| `AnalyticsClickHouseRepository` | Выполняет SQL-агрегацию `globalCount`, `referCount`, `averageRank`. |

## DTO

| Компонент | Назначение |
| --- | --- |
| `AnalyticsResponse` | REST DTO для ответа клиенту. Поля представлены строками. |
| `AnalyticsWsMessage` | Обертка для WebSocket-сообщения: тип, `manufactureId`, время генерации, аналитический снапшот. |
| `RequestedEvent` | Контракт Kafka-события показов/попаданий в выдачу. |
| `ReferedEvent` | Контракт Kafka-события переходов на карточку. |

## WebSocket

| Компонент | Назначение |
| --- | --- |
| `AnalyticsWebSocketConfig` | Регистрирует endpoint `/ws/analytics` и scheduler для публикаций. |
| `AnalyticsHandshakeInterceptor` | Проверяет API key и извлекает `manufactureId` из query string. |
| `AnalyticsWebSocketHandler` | Хранит подписчиков по `manufactureId` и рассылает готовые сообщения. |

## Frontend Monitor

| Компонент | Назначение |
| --- | --- |
| `MonitorPanel` | Основной экран мониторинга: ввод API key и UUID, выбор режима, отображение JSON и графика. |
| `App` | Тонкая обертка, которая монтирует `MonitorPanel`. |
| `main.jsx` | Инициализация React-приложения и глобальных стилей панели. |

## Примечание

В список включены только компоненты, которые реально участвуют в текущем аналитическом контуре. Второстепенные конфиги и служебные классы без прямого влияния на поток данных здесь не перечислены.
