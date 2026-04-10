# 2.7. Ключевые программные компоненты и фрагменты реализации

### REST-контроллер аналитики
Назначение: Показывает основной HTTP-интерфейс аналитического модуля. Этот фрагмент удобно использовать для иллюстрации публичного API и набора endpoint'ов для получения метрик.
Путь: modules/analytic/src/main/java/ru/ya/analytic/adapters/in/http/AnalyticsController.java
Файл: AnalyticsController.java
Строка начала: 43
Код:
```java
@GetMapping("/{manufactureId}/show")
@Operation(
        summary = "Показать всю аналитику для товара",
        description = "Возвращает объект с averageRank, globalCount, referCount"
)
public ResponseEntity<AnalyticsResponse> getAnalyticShowDTO(
        @Parameter(description = "UUID товара") @PathVariable UUID manufactureId,
        @RequestHeader(name = "X-API-KEY", required = false) String key
) {
    if (!isValidApiKey(key)) return forbiddenResponse();
    return ResponseEntity.ok(useCase.getAnalyticShowDTO(manufactureId));
}

@GetMapping("/{manufactureId}/avg")
@Operation(summary = "Средний ранг товара", description = "Возвращает объект с averageRank")
public ResponseEntity<AnalyticsResponse> getAnalyticAvgDTO(
        @Parameter(description = "UUID товара") @PathVariable UUID manufactureId,
        @RequestHeader(name = "X-API-KEY", required = false) String key
) {
    if (!isValidApiKey(key)) return forbiddenResponse();
    return ResponseEntity.ok(useCase.getAnalyticAvgDTO(manufactureId));
}

@GetMapping("/{manufactureId}/refer")
@Operation(summary = "Количество рефералов товара", description = "Возвращает объект с referCount")
public ResponseEntity<AnalyticsResponse> getAnalyticReferDTO(
        @Parameter(description = "UUID товара") @PathVariable UUID manufactureId,
        @RequestHeader(name = "X-API-KEY", required = false) String key
) {
    if (!isValidApiKey(key)) return forbiddenResponse();
    return ResponseEntity.ok(useCase.getAnalyticReferDTO(manufactureId));
}

@GetMapping("/{manufactureId}/count")
@Operation(summary = "Общее количество показов товара", description = "Возвращает объект с globalCount")
public ResponseEntity<AnalyticsResponse> getAnalyticCountDTO(
        @Parameter(description = "UUID товара") @PathVariable UUID manufactureId,
        @RequestHeader(name = "X-API-KEY", required = false) String key
) {
    if (!isValidApiKey(key)) return forbiddenResponse();
    return ResponseEntity.ok(useCase.getAnalyticCountDTO(manufactureId));
}
```

### DTO ответа AnalyticsResponse
Назначение: Показывает формат данных, который возвращает аналитический REST API. Фрагмент подходит для описания структуры ответа backend-сервиса.
Путь: modules/analytic/src/main/java/ru/ya/analytic/adapters/in/dto/AnalyticsResponse.java
Файл: AnalyticsResponse.java
Строка начала: 8
Код:
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse{
    private String averageRank; // Средняя позиция в каталоге
    private String globalCount; // Показы в поиске и каталоге (попадал в выборку)
    private String referCount;  // Переходы на карточки
}
```

### Kafka consumer аналитического модуля
Назначение: Показывает точку входа событий в backend и связывает Kafka с загрузкой данных и WebSocket-обновлениями. Это один из ключевых фрагментов для демонстрации событийной архитектуры.
Путь: modules/analytic/src/main/java/ru/ya/analytic/adapters/in/kafka/AnalyticsKafkaListener.java
Файл: AnalyticsKafkaListener.java
Строка начала: 31
Код:
```java
@KafkaListener(
        topics = "${kafka.topic.requested}",
        groupId = "analytics-service"
)
public void onRequestedEvent(Map<String, Object> message) {
    log.info("[Listener] Received message on 'market-requested': {}", message);

    try {
        RequestedEvent event = objectMapper.convertValue(message, RequestedEvent.class);
        if (loadDataUseCase.loadRequested(event)) {
            analyticsWebSocketPublisher.scheduleSnapshot(event.getManufactureId());
        }
    } catch (Exception e) {
        log.error("Error processing RequestedEvent: {}", message, e);
    }
}

@KafkaListener(
        topics = "${kafka.topic.referred}",
        groupId = "analytics-service"
)
public void onReferredEvent(Map<String, Object> message) {
    log.info("[Listener] Received message on 'market-referred': {}", message);

    try {
        ReferedEvent event = objectMapper.convertValue(message, ReferedEvent.class);
        if (loadDataUseCase.loadReferred(event)) {
            analyticsWebSocketPublisher.scheduleSnapshot(event.getManufactureId());
        }
    } catch (Exception e) {
        log.error("Error processing ReferedEvent: {}", message, e);
    }
}
```

### Kafka producer симулятора
Назначение: Показывает, как тестовые события публикуются в Kafka-топики. Фрагмент полезен для иллюстрации источника данных аналитического контура.
Путь: modules/simulator/src/main/java/ru/ya/simulator/infrastructure/kafka/KafkaEventPublisher.java
Файл: KafkaEventPublisher.java
Строка начала: 22
Код:
```java
public void sendRequested(RequestedEvent event) {
    kafkaTemplate.send(requestedTopic, event.getCategoryId().toString(), event);
}

public void sendReferred(ReferedEvent event) {
    kafkaTemplate.send(referredTopic, event.getManufactureId().toString(), event);
}
```

### Запись событий в ClickHouse
Назначение: Показывает, как backend сохраняет входящие события в аналитическое хранилище. Это ключевой фрагмент участка Kafka → backend → ClickHouse.
Путь: modules/analytic/src/main/java/ru/ya/analytic/adapters/out/clickhouse/DataLoadClickHouseRepository.java
Файл: DataLoadClickHouseRepository.java
Строка начала: 20
Код:
```java
private static final String SQL_INSERT_REQUESTED = """
    INSERT INTO analytic.requested (event_date, cid, mid, count)
    VALUES (?, ?, ?, ?)
    """;

private static final String SQL_INSERT_REFERED = """
    INSERT INTO analytic.refered (event_date, mid, count)
    VALUES (?, ?, ?)
    """;

@Override
public boolean loadRequested(RequestedEvent event) {
    log.debug("Saving RequestedEvent: {}", event);

    clickHouseJdbcTemplate.update(
            SQL_INSERT_REQUESTED,
            Date.valueOf(event.getEventDate()),
            event.getCategoryId().toString(),
            event.getManufactureId().toString(),
            event.getCount()
    );

    return true;
}

@Override
public boolean loadReferred(ReferedEvent event) {
    log.debug("Saving ReferedEvent: {}", event);

    clickHouseJdbcTemplate.update(
            SQL_INSERT_REFERED,
            Date.valueOf(event.getEventDate()),
            event.getManufactureId().toString(),
            event.getCount()
    );

    return true;
}
```

### Сервис агрегации метрик для полного ответа
Назначение: Показывает сервисный слой, который собирает итоговый DTO из нескольких метрик. Фрагмент хорошо иллюстрирует разделение между API-слоем и слоем доступа к данным.
Путь: modules/analytic/src/main/java/ru/ya/analytic/application/usecase/AnalyticsService.java
Файл: AnalyticsService.java
Строка начала: 17
Код:
```java
@Override
public AnalyticsResponse getAnalyticShowDTO(UUID mid) {
    // агрегируем все метрики на сервисном уровне
    Double avg = analyticsPort.getAverageRank(mid);
    Long total = analyticsPort.getTotalCount(mid);
    Integer refer = analyticsPort.getReferCount(mid);

    return AnalyticsResponse.builder()
            .averageRank(avg.toString())
            .globalCount(total.toString())
            .referCount(refer.toString())
            .build();
}
```

### SQL-расчёт averageRank, globalCount и referCount
Назначение: Показывает, где именно в проекте вычисляются аналитические метрики. Этот фрагмент напрямую демонстрирует логику получения показателей из ClickHouse.
Путь: modules/analytic/src/main/java/ru/ya/analytic/adapters/out/clickhouse/AnalyticsClickHouseRepository.java
Файл: AnalyticsClickHouseRepository.java
Строка начала: 22
Код:
```java
private static final String SQL_AVG_RANK = """
    SELECT avg(avg_rank) AS average_rank
    FROM (
             SELECT
                 cid,
                 avg(rank) AS avg_rank
             FROM (
                      SELECT
                          cid,
                          row_number() OVER (PARTITION BY cid ORDER BY count DESC) AS rank
                      FROM analytic.requested
                  ) AS ranked
             WHERE cid IN (
                 SELECT DISTINCT cid
                 FROM analytic.requested
                 WHERE mid = ?
             )
             GROUP BY cid
         ) as car
    """;

private static final String SQL_TOTAL_COUNT = """
    SELECT sum(count) AS total_count
    FROM analytic.requested
    WHERE mid = ?
    """;

private static final String SQL_REFER_COUNT = """
    SELECT sum(count) AS refer_count
    FROM analytic.refered
    WHERE mid = ?
    """;
```

### Параметры подключения к ClickHouse
Назначение: Показывает минимальную конфигурацию подключения backend-сервиса к аналитической БД. Фрагмент удобен для раздела про инфраструктурные зависимости модуля.
Путь: modules/analytic/src/main/resources/application.properties
Файл: application.properties
Строка начала: 5
Код:
```properties
# Clickhouse
spring.datasource.clickhouse.url=jdbc:clickhouse://${CLICKHOUSE_HOST:clickhouse}:${CLICKHOUSE_HTTP_PORT:8123}/default?compress=false
spring.datasource.clickhouse.username=${CLICKHOUSE_USER:clickhouse}
spring.datasource.clickhouse.password=${CLICKHOUSE_PASSWORD:root}
spring.datasource.clickhouse.driver-class-name=com.clickhouse.jdbc.ClickHouseDriver
```

### Docker Compose: Kafka
Назначение: Показывает конфигурацию брокера сообщений, через который строится событийный контур. Этот фрагмент полезен для раздела о развёртывании архитектуры.
Путь: docker-compose.yml
Файл: docker-compose.yml
Строка начала: 21
Код:
```yaml
kafka:
  image: confluentinc/cp-kafka:7.4.0
  container_name: kafka
  ports:
    - "9092:9092"
  environment:
    KAFKA_BROKER_ID: 1
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181

    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
    KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,PLAINTEXT_HOST://0.0.0.0:29092
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:29092

    KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"

  depends_on:
    - zookeeper

  healthcheck:
    test: [ "CMD", "bash", "-c", "echo > /dev/tcp/kafka/9092" ]
    interval: 10s
    timeout: 5s
    retries: 10

  networks:
    - app-network
```

### Docker Compose: backend аналитики
Назначение: Показывает, как аналитический сервис поднимается в контейнере и связывается с Kafka. Фрагмент хорошо иллюстрирует место backend-компонента в общей схеме развертывания.
Путь: docker-compose.yml
Файл: docker-compose.yml
Строка начала: 71
Код:
```yaml
analytic:
  build:
    context: ./modules/analytic
    dockerfile: Dockerfile
  container_name: analytic
  ports:
    - "8001:8080"
  depends_on:
    kafka:
      condition: service_healthy
  networks:
    - app-network
```

### Docker Compose: frontend и ClickHouse
Назначение: Показывает сервис визуализации и аналитическое хранилище в составе общего контура. Этот фрагмент уместен для схемы развертывания мониторинга и БД.
Путь: docker-compose.yml
Файл: docker-compose.yml
Строка начала: 103
Код:
```yaml
frontend:
  build:
    context: ./modules/analytic-monitor
    dockerfile: Dockerfile
  container_name: analytic-monitor
  ports:
    - "5173:5173"
  depends_on:
    - analytic
    - simulator
  networks:
    - app-network

# ---------------------------------------------------------------------------
# ClickHouse
# ---------------------------------------------------------------------------
clickhouse:
  image: clickhouse/clickhouse-server:latest
  container_name: clickhouse
  ports:
    - "8123:8123"
    - "9000:9000"
  environment:
    - CLICKHOUSE_USER=clickhouse
    - CLICKHOUSE_PASSWORD=root
  ulimits:
    nofile:
      soft: 262144
      hard: 262144
  restart: unless-stopped
  networks:
    - app-network
```

### Конфигурация WebSocket endpoint
Назначение: Показывает регистрацию WebSocket-канала аналитики и его привязку к frontend origin. Фрагмент подходит для демонстрации push-механизма в модуле.
Путь: modules/analytic/src/main/java/ru/ya/analytic/config/AnalyticsWebSocketConfig.java
Файл: AnalyticsWebSocketConfig.java
Строка начала: 28
Код:
```java
@Override
public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    String allowedOrigin = "http://" + frontendHost + ":" + frontendPort;

    registry.addHandler(analyticsWebSocketHandler, "/ws/analytics")
            .addInterceptors(analyticsHandshakeInterceptor)
            .setAllowedOrigins(allowedOrigin);
}

@Bean
public ThreadPoolTaskScheduler analyticsWebSocketTaskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(2);
    scheduler.setThreadNamePrefix("analytics-ws-");
    scheduler.initialize();
    return scheduler;
}
```

### Публикация аналитического snapshot по WebSocket
Назначение: Показывает, как после обработки события формируется и отправляется push-обновление клиенту. Это компактный фрагмент для иллюстрации расширения REST API механизмом real-time доставки.
Путь: modules/analytic/src/main/java/ru/ya/analytic/application/usecase/AnalyticsWebSocketPublisher.java
Файл: AnalyticsWebSocketPublisher.java
Строка начала: 34
Код:
```java
public void scheduleSnapshot(UUID manufactureId) {
    if (!analyticsWebSocketHandler.hasSubscribers(manufactureId)) {
        return;
    }

    AtomicReference<ScheduledFuture<?>> futureReference = new AtomicReference<>();
    ScheduledFuture<?> future = analyticsWebSocketTaskScheduler.schedule(
            () -> {
                try {
                    publishSnapshot(manufactureId);
                } finally {
                    pendingPublishes.remove(manufactureId, futureReference.get());
                }
            },
            Instant.now().plusMillis(publishDelayMs)
    );
    futureReference.set(future);

    ScheduledFuture<?> previousFuture = pendingPublishes.put(manufactureId, future);
    if (previousFuture != null) {
        previousFuture.cancel(false);
    }
}

private void publishSnapshot(UUID manufactureId) {
    if (!analyticsWebSocketHandler.hasSubscribers(manufactureId)) {
        return;
    }

    try {
        AnalyticsResponse analytics = analyticsUseCase.getAnalyticShowDTO(manufactureId);
        analyticsWebSocketHandler.broadcast(new AnalyticsWsMessage(
                "analytics.snapshot",
                manufactureId,
                Instant.now(),
                analytics
        ));
    } catch (Exception ex) {
        log.error("Failed to publish analytics snapshot for {}", manufactureId, ex);
    }
}
```
