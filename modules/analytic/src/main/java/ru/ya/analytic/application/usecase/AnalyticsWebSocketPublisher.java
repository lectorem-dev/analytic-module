package ru.ya.analytic.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import ru.ya.analytic.adapters.in.dto.AnalyticsResponse;
import ru.ya.analytic.adapters.in.dto.AnalyticsWsMessage;
import ru.ya.analytic.adapters.in.websocket.AnalyticsWebSocketHandler;
import ru.ya.analytic.application.in.GetAnalyticsUseCase;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsWebSocketPublisher {

    private final GetAnalyticsUseCase analyticsUseCase;
    private final AnalyticsWebSocketHandler analyticsWebSocketHandler;
    private final TaskScheduler analyticsWebSocketTaskScheduler;

    private final ConcurrentMap<UUID, ScheduledFuture<?>> pendingPublishes = new ConcurrentHashMap<>();

    @Value("${analytics.ws.publish-delay-ms:300}")
    private long publishDelayMs;

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
}
