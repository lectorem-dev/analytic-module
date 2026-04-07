# Актуальное состояние проекта

> Основание анализа: рабочее дерево репозитория на 2026-04-07, а не только последний коммит `HEAD`. Это важно, потому что в рабочем каталоге уже есть незакоммиченные изменения, добавляющие WebSocket-канал и dual-mode frontend, и они успешно собираются.

## 1. Краткое summary проекта

Проект представляет собой контейнеризированный MVP аналитического модуля для маркетплейс-подобного сценария.  
Его текущая задача по факту: генерировать синтетические пользовательские события, передавать их через Kafka, сохранять их в ClickHouse, вычислять несколько агрегированных метрик и показывать результат через API и веб-панель.  
В репозитории реально существуют четыре прикладных модуля: `analytic`, `simulator`, `libs-analytic` и `analytic-monitor`.  
В `docker-compose.yml` запускаются `analytic`, `simulator`, `frontend`, `kafka`, `zookeeper`, `kafka-ui`, `clickhouse` и `ch-ui`.  
Источник данных в текущем срезе не внешний маркетплейс и не PostgreSQL, а встроенный `simulator`, который по расписанию генерирует `RequestedEvent` и `ReferedEvent`.  
Эти события публикуются в Kafka-топики `market-requested` и `market-referred`, после чего `analytic` читает их Kafka-листенерами и вставляет в ClickHouse через JDBC.  
API аналитики реализован как набор REST `GET`-эндпоинтов по `manufactureId` с ручной проверкой заголовка `X-API-KEY`.  
Поверх REST в текущем рабочем дереве реализован WebSocket `/ws/analytics`, который пушит обновленные снапшоты по конкретному `manufactureId`, если есть подписчики.  
Frontend-панель умеет работать в двух режимах: `REST polling` и `WebSocket`; переключение задается вручную в интерфейсе.  
MVP по факту включает генерацию тестовых событий, потоковую доставку через Kafka, хранение сырых данных в ClickHouse, расчет трех метрик (`globalCount`, `referCount`, `averageRank`), REST API, WebSocket-уведомления и простую визуализацию графика.  
PostgreSQL, синхронизация PostgreSQL -> ClickHouse, SQL-миграции, batch ETL и полноценный security-слой в текущем коде отсутствуют.  
Автотесты и Testcontainers в репозитории не обнаружены, но backend и frontend в текущем рабочем дереве успешно собираются.

## 2. Фактическая архитектура

### Компоненты

- `modules/analytic`  
  Основной backend-сервис аналитики. Читает события из Kafka, пишет сырые данные в ClickHouse, считает метрики SQL-запросами, отдает REST API и WebSocket.
- `modules/simulator`  
  Генератор синтетических событий. По расписанию создает категории и набор идентификаторов `manufactureId`, генерирует просмотры и переходы и публикует их в Kafka.
- `modules/libs-analytic`  
  Общая библиотека моделей событий `RequestedEvent` и `ReferedEvent`, используемая `simulator` и `analytic`.
- `modules/analytic-monitor`  
  React/Vite-панель наблюдения. Делает REST polling или открывает WebSocket-соединение и строит график по истории значений.
- `kafka` + `zookeeper`  
  Брокер сообщений и его инфраструктура в compose.
- `kafka-ui`  
  Вспомогательный UI для просмотра Kafka.
- `clickhouse`  
  Аналитическое хранилище, куда `analytic` пишет события и откуда читает агрегаты.
- `ch-ui`  
  Вспомогательный UI для просмотра ClickHouse.

### Роль хранилищ и брокера

- Kafka используется как текущий транспорт событий между `simulator` и `analytic`.
- ClickHouse используется как единственное аналитическое хранилище в текущем коде.
- PostgreSQL в текущем проекте не используется вообще: нет зависимостей, конфигов, compose-сервиса и кода интеграции.

### Схема потока данных

`GeneratorConfigInitializer` при старте `simulator` генерирует случайные `categoryId` и `manufactureId`.  
`EventGeneratorService` по `@Scheduled(fixedRateString = "${simulator.generate-interval-ms}")` публикует `RequestedEvent` для всех элементов ранжированного списка и иногда `ReferedEvent` для случайно выбранного элемента.  
`KafkaEventPublisher` пишет эти события в Kafka-топики `market-requested` и `market-referred`.  
`AnalyticsKafkaListener` в сервисе `analytic` читает оба топика, десериализует payload в модели из `libs-analytic` и передает их в `LoadDataUseCase`.  
`DataLoadClickHouseRepository` вставляет события в таблицы ClickHouse `analytic.requested` и `analytic.refered`.  
`AnalyticsClickHouseRepository` по запросу считает `globalCount`, `referCount` и `averageRank` SQL-запросами к ClickHouse.  
`AnalyticsController` отдает REST-ответы `/api/{manufactureId}/show|avg|refer|count`, проверяя `X-API-KEY`.  
В текущем рабочем дереве `AnalyticsWebSocketPublisher` после записи события планирует публикацию снапшота в `/ws/analytics` для подписчиков конкретного `manufactureId`.  
`analytic-monitor` либо периодически вызывает REST `/show`, либо держит WebSocket и рисует историю метрик на графике.

### Что откуда берется и куда записывается

- Справочники товаров/производителей не приходят из внешней БД. Они синтетически генерируются внутри `simulator` при старте.
- Сырые события не пишутся в PostgreSQL и не проходят ETL-пайплайн. Они сразу идут из `simulator` в Kafka, а затем из Kafka в ClickHouse.
- Таблицы ClickHouse создаются самим `analytic` через `ApplicationRunner`; отдельных SQL init/migration-файлов нет.
- Метрики не материализуются заранее и не складываются в отдельную агрегатную таблицу; они вычисляются on-demand SQL-запросами из REST/WS сценария.

## 3. Реально реализованный функционал

Ниже перечислено только то, что подтверждается кодом, compose-конфигами или сборкой.

- Многомодульный Gradle-проект с backend-сервисом аналитики, симулятором событий, библиотекой общих моделей и frontend-панелью.
- Docker Compose на 8 сервисов: `zookeeper`, `kafka`, `kafka-ui`, `simulator`, `analytic`, `clickhouse`, `ch-ui`, `frontend`.
- Планировщик событий в `simulator` на `@Scheduled`, интервал задается `simulator.generate-interval-ms`.
- Два Kafka-топика: `market-requested` и `market-referred`.
- Публикация в Kafka синтетических событий `RequestedEvent(categoryId, manufactureId, count, eventDate)` и `ReferedEvent(manufactureId, count, eventDate)`.
- Потребление этих событий в `analytic` через `@KafkaListener`.
- Автосоздание базы `analytic` и таблиц `analytic.requested` и `analytic.refered` в ClickHouse на старте backend-сервиса.
- Запись событий в ClickHouse через `JdbcTemplate`.
- REST API:
  - `GET /api/{manufactureId}/show`
  - `GET /api/{manufactureId}/avg`
  - `GET /api/{manufactureId}/refer`
  - `GET /api/{manufactureId}/count`
- Проверка API-ключа через заголовок `X-API-KEY` внутри REST-контроллера.
- WebSocket-эндпоинт `/ws/analytics` в текущем рабочем дереве с обязательными `manufactureId` и `apiKey`.
- Debounce-публикация снапшотов аналитики через `TaskScheduler` после прихода новых Kafka-событий.
- Frontend-панель на React + Chart.js:
  - ручной ввод `API Key`
  - ручной ввод `manufactureId`
  - выбор режима `REST polling` или `WebSocket`
  - настройка интервала polling для REST
  - вывод текущего JSON-ответа и графика истории
- CORS, ограниченный `http://localhost:5173`.
- Swagger/OpenAPI dependency и аннотации на REST-контроллере.
- Вспомогательный REST `GET /simulator/config` для чтения текущей случайно сгенерированной конфигурации симулятора.

### Что именно считается как аналитика

- `globalCount`  
  Сумма поля `count` по всем записям `analytic.requested` для указанного `mid` / `manufactureId`.
- `referCount`  
  Сумма поля `count` по всем записям `analytic.refered` для указанного `mid` / `manufactureId`.
- `averageRank`  
  Вычисляется SQL-запросом по `analytic.requested` через `row_number() over (partition by cid order by count desc)` и последующее усреднение. Метрика есть в коде, но это не фильтрованный по времени и не materialized rank-срез.

### Что не подтверждается кодом

- PostgreSQL или любой другой OLTP-источник.
- Синхронизация PostgreSQL -> ClickHouse.
- SQL-миграции через Flyway/Liquibase.
- Batch/JDBC ETL из внешней БД.
- Фильтрация API по категории, времени, seller, session.
- Автотесты, integration tests, Testcontainers.
- WebSocket STOMP/SockJS; используется raw Spring WebSocket handler.

## 4. Что изменялось по ходу развития проекта

### Ранний вариант

- Проект стартовал как одномодульный Spring Boot сервис без Kafka и без симулятора.
- В начальном коммите использовался `spring-boot-starter-webflux`.
- События загружались напрямую через REST `POST /api/load/requested` и `POST /api/load/referred`.
- Compose тогда поднимал только `analytic-module`, `clickhouse` и `ch-ui`.
- Глобальная проверка API-ключа была вынесена в `ApiKeyFilter` на `WebFilter`.

### Промежуточный вариант

- Затем проект был переведен в multi-module Gradle-структуру: появились `modules/analytic`, `modules/simulator`, `modules/libs-analytic`.
- Вместо прямой REST-загрузки был введен Kafka-пайплайн между `simulator` и `analytic`.
- Появились Kafka-топики, Kafka producer/consumer и общие event-модели в `libs-analytic`.
- Инфраструктура Kafka по истории менялась: был этап с KRaft-mode Kafka, затем compose переключили на классическую Kafka + Zookeeper.
- Позже появился frontend `analytic-monitor`, но в последнем закоммиченном варианте он работал только через REST polling.
- Глобальный `ApiKeyFilter` был удален, а проверка ключа переехала в сам REST-контроллер.

### Текущее состояние

- В текущем рабочем дереве поверх последнего коммита добавлен backend WebSocket-канал `/ws/analytics`.
- `AnalyticsKafkaListener` теперь после записи события инициирует отложенную публикацию снапшота через `AnalyticsWebSocketPublisher`.
- Frontend-панель умеет переключаться между `REST polling` и `WebSocket`.
- Тем самым текущий фактический pipeline стал не только request/response, но и event-driven до уровня клиентской визуализации.

### Что именно эволюционировало

- Ingestion событий: `REST load` -> `Kafka`.
- Структура проекта: `single-module` -> `multi-module`.
- Источник данных: вручную загружаемые списки событий -> встроенный планировщик-симулятор.
- UI-модель: отсутствовал -> frontend с REST polling -> frontend с REST polling и WebSocket.
- Security: ранний глобальный filter -> ручная проверка API key в контроллере -> ручная проверка API key в REST и WebSocket handshake.
- Kafka infra: попытка KRaft -> классическая Kafka + Zookeeper.

### Расхождения между кодом и документами

- `old-readme.md` устарел сильнее всего: он описывает прием событий через API и не соответствует текущему Kafka-ingestion.
- `old-readme-2.md` утверждает фильтрацию по товарам, категориям и времени, но таких фильтров в коде нет.
- `README.md` перечисляет `Testcontainers`, но в зависимостях и тестах их нет.
- `README.md` заявляет `Hexagonal Architecture`; по структуре пакетов это частично похоже на ports-and-adapters, но реализация не строго hexagonal.
- `.env` содержит `API_KEY=my-secret-key`, но текущий `docker-compose.yml` не пробрасывает `API_KEY` в контейнер `analytic`; в compose-runtime сервис использует дефолт `secret`, если переменная не задана внешне.
- Последний коммит `HEAD` еще не отражает WebSocket-слой; он есть только в текущем рабочем дереве.

## 5. Спорные и потенциально ложные формулировки для ВКР

| Формулировка | Статус | Почему | Как написать безопасно |
| --- | --- | --- | --- |
| «Все события идут через Kafka» | `partially true` | В текущем коде ingestion `RequestedEvent` и `ReferedEvent` действительно идет только через Kafka, но исторически ранний вариант принимал события по REST. | «В текущем состоянии проекта пользовательские события для аналитического контура поступают в `analytic` через Kafka-топики `market-requested` и `market-referred`. На раннем этапе использовалась REST-загрузка событий.» |
| «Есть синхронизация PostgreSQL -> ClickHouse» | `false` | PostgreSQL нигде не найден: нет зависимостей, compose-сервиса, datasource и кода синхронизации. | «В текущем MVP ClickHouse заполняется напрямую из Kafka-контура через backend-сервис `analytic`; PostgreSQL в контуре не используется.» |
| «Используется WebSocket» | `partially true` | В рабочем дереве WebSocket реализован и собирается, но в последнем коммите `HEAD` frontend был только на REST polling. | «В актуальном рабочем дереве реализован WebSocket-канал для push-обновлений; в закоммиченной версии до этих изменений визуализация работала через REST polling.» |
| «Архитектура является hexagonal» | `partially true` | Есть пакеты `application/in`, `application/out`, `application/usecase`, `adapters/in`, `adapters/out`, но границы нестрогие: use case возвращает DTO из adapter-layer, инфраструктурные детали местами протекают в приложение. | «Backend использует элементы ports-and-adapters / layered structuring, но называть реализацию строго hexagonal без оговорок не стоит.» |
| «Это microservice-based architecture» | `partially true` | Контейнеров несколько, но доменная логика сосредоточена в одном аналитическом backend; `simulator` и `frontend` скорее вспомогательные сервисы для демонстрации MVP. | «Проект представляет собой контейнеризированную многокомпонентную MVP-систему с одним основным аналитическим сервисом и вспомогательными сервисами генерации и визуализации.» |
| «API поддерживает фильтрацию по категориям и времени» | `false` | В REST нет query-параметров, только `manufactureId` в path. В SQL нет параметризованной фильтрации по времени или категории. | «Текущий API возвращает метрики только для заданного `manufactureId`; фильтры по категории и времени пока не реализованы.» |
| «В аналитике используются товар, производитель, категория, seller, session» | `partially true` | В коде реально есть только `categoryId`, `manufactureId`, `count`, `eventDate`. `seller` и `session` отсутствуют. Терминологически `manufactureId` в UI и README местами называется товаром, что не совпадает с именованием модели. | «Текущая аналитика опирается на `categoryId` и `manufactureId` как ключ анализируемой сущности; seller/session в коде не используются.» |
| «MVP уже интегрирован с реальным маркетплейсом» | `false` | Источник данных сейчас встроенный `simulator`, который случайно генерирует события. | «Текущий MVP демонстрирует архитектурный контур на синтетических событиях, генерируемых встроенным симулятором.» |
| «API защищен Spring Security» | `false` | Нет `spring-boot-starter-security`, `SecurityFilterChain`, JWT или role-based auth. Есть только ручная проверка `X-API-KEY` в контроллере и WebSocket handshake. | «В текущем MVP реализована только базовая проверка API key; полноценный security-layer не внедрен.» |
| «Используются Testcontainers и покрытые тестами сценарии» | `false` | Тестов в `src/test` нет, зависимостей Testcontainers тоже нет. | «Сборка проекта подтверждена, но автоматические тесты и Testcontainers в текущем репозитории отсутствуют.» |
| «`averageRank` — это точная средняя позиция товара в каталоге» | `partially true` | Метрика `averageRank` в коде есть, но она считается специфическим SQL-запросом по историческим строкам `analytic.requested` без временной фильтрации и без отдельной витрины рангов. | «В MVP рассчитывается метрика `averageRank`, получаемая SQL-агрегацией по данным `requested`; ее лучше описывать как вычисляемую оценку ранга, а не как точный online-rank витрины.» |

## 6. Безопасное итоговое описание проекта для ВКР

Проект представляет собой MVP аналитического модуля для маркетплейс-подобной системы, предназначенный для демонстрации полного контура потоковой обработки событий: от генерации пользовательских действий до получения агрегированных метрик и их визуализации. В текущем состоянии система состоит из аналитического backend-сервиса на Spring Boot, генератора синтетических событий, Kafka-брокера, ClickHouse-хранилища и React-панели мониторинга. События `RequestedEvent` и `ReferedEvent` публикуются симулятором в Kafka, затем сервис `analytic` считывает их, сохраняет в ClickHouse и по запросу вычисляет метрики `globalCount`, `referCount` и `averageRank`.

Пользовательский доступ к результатам реализован через REST API с проверкой `X-API-KEY`, а в актуальном рабочем срезе также через WebSocket-канал для push-обновлений аналитики. Веб-панель поддерживает оба режима визуализации: периодический REST polling и получение обновлений по WebSocket. Проект не использует PostgreSQL, batch ETL и полноценный security-layer; в текущем MVP акцент сделан на демонстрации Kafka-ClickHouse-контура, базового API и наглядной визуализации метрик на синтетических данных.

## 7. Список подтверждающих артефактов

### Backend и доменная логика

- `modules/analytic/src/main/java/ru/ya/analytic/adapters/in/kafka/AnalyticsKafkaListener.java`
- `modules/analytic/src/main/java/ru/ya/analytic/adapters/out/clickhouse/DataLoadClickHouseRepository.java`
- `modules/analytic/src/main/java/ru/ya/analytic/adapters/out/clickhouse/AnalyticsClickHouseRepository.java`
- `modules/analytic/src/main/java/ru/ya/analytic/adapters/in/http/AnalyticsController.java`
- `modules/analytic/src/main/java/ru/ya/analytic/application/usecase/AnalyticsService.java`
- `modules/analytic/src/main/java/ru/ya/analytic/application/usecase/LoadDataService.java`
- `modules/analytic/src/main/java/ru/ya/analytic/config/ClickHouseConfig.java`
- `modules/analytic/src/main/resources/application.properties`

### WebSocket-слой текущего рабочего дерева

- `modules/analytic/src/main/java/ru/ya/analytic/config/AnalyticsWebSocketConfig.java`
- `modules/analytic/src/main/java/ru/ya/analytic/application/usecase/AnalyticsWebSocketPublisher.java`
- `modules/analytic/src/main/java/ru/ya/analytic/adapters/in/websocket/AnalyticsHandshakeInterceptor.java`
- `modules/analytic/src/main/java/ru/ya/analytic/adapters/in/websocket/AnalyticsWebSocketHandler.java`
- `modules/analytic/src/main/java/ru/ya/analytic/adapters/in/dto/AnalyticsWsMessage.java`
- `git status --short` и `git diff` относительно `HEAD`, подтверждающие, что этот слой есть в рабочем дереве, но еще не закоммичен

### Simulator и модели событий

- `modules/simulator/src/main/java/ru/ya/simulator/application/EventGeneratorService.java`
- `modules/simulator/src/main/java/ru/ya/simulator/infrastructure/kafka/KafkaEventPublisher.java`
- `modules/simulator/src/main/java/ru/ya/simulator/config/GeneratorConfigInitializer.java`
- `modules/simulator/src/main/java/ru/ya/simulator/controller/SimulatorConfigController.java`
- `modules/libs-analytic/src/main/java/ru/ya/libs/model/RequestedEvent.java`
- `modules/libs-analytic/src/main/java/ru/ya/libs/model/ReferedEvent.java`

### Frontend / dashboard

- `modules/analytic-monitor/src/components/MonitorPanel.jsx`
- `modules/analytic-monitor/src/App.jsx`
- `modules/analytic-monitor/src/main.jsx`
- `modules/analytic-monitor/package.json`
- `modules/analytic-monitor/Dockerfile`
- `modules/analytic-monitor/nginx.conf`

### Infra и запуск

- `docker-compose.yml`
- `.env`
- `modules/analytic/Dockerfile`
- `modules/simulator/Dockerfile`

### Документы и история

- `README.md`
- `old-readme.md`
- `old-readme-2.md`
- `doc/frontend.png`
- `git log --oneline`
- ключевые коммиты эволюции:
  - `4d391c1` — initial single-module REST-loading variant
  - `0e640eb` — переход на multi-module
  - `cff4ded` — добавление Kafka/simulator/libs
  - `d26d1d6` и `4b77c94` — изменения Kafka infra
  - `bf8ad99` и `527c10f` — frontend + ручная API-key логика

### Что дополнительно было проверено вручную

- `./gradlew build` — успешно
- `npm run build` в `modules/analytic-monitor` — успешно
- `docker compose config --services` — подтверждает состав запускаемых сервисов
