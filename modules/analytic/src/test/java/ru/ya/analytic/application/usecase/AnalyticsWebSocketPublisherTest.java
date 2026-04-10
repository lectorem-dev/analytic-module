package ru.ya.analytic.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;
import ru.ya.analytic.adapters.in.dto.AnalyticsResponse;
import ru.ya.analytic.adapters.in.dto.AnalyticsWsMessage;
import ru.ya.analytic.adapters.in.websocket.AnalyticsWebSocketHandler;
import ru.ya.analytic.application.in.GetAnalyticsUseCase;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsWebSocketPublisherTest {

    @Mock
    private GetAnalyticsUseCase analyticsUseCase;

    @Mock
    private AnalyticsWebSocketHandler analyticsWebSocketHandler;

    @Mock
    private TaskScheduler analyticsWebSocketTaskScheduler;

    @InjectMocks
    private AnalyticsWebSocketPublisher publisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(publisher, "publishDelayMs", 1L);
    }

    @Test
    void scheduleSnapshotSkipsSchedulingWithoutSubscribers() {
        UUID manufactureId = UUID.randomUUID();
        when(analyticsWebSocketHandler.hasSubscribers(manufactureId)).thenReturn(false);

        publisher.scheduleSnapshot(manufactureId);

        verifyNoInteractions(analyticsWebSocketTaskScheduler, analyticsUseCase);
        verify(analyticsWebSocketHandler, never()).broadcast(any());
    }

    @Test
    void scheduledRunnablePublishesSnapshotForSubscribers() {
        UUID manufactureId = UUID.randomUUID();
        AnalyticsResponse analytics = AnalyticsResponse.builder()
                .averageRank("4.0")
                .globalCount("18")
                .referCount("7")
                .build();
        ScheduledFuture<?> future = org.mockito.Mockito.mock(ScheduledFuture.class);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        when(analyticsWebSocketHandler.hasSubscribers(manufactureId)).thenReturn(true);
        when(analyticsUseCase.getAnalyticShowDTO(manufactureId)).thenReturn(analytics);
        doReturn(future)
                .when(analyticsWebSocketTaskScheduler)
                .schedule(runnableCaptor.capture(), any(Instant.class));

        publisher.scheduleSnapshot(manufactureId);
        runnableCaptor.getValue().run();

        ArgumentCaptor<AnalyticsWsMessage> messageCaptor = ArgumentCaptor.forClass(AnalyticsWsMessage.class);
        verify(analyticsWebSocketHandler).broadcast(messageCaptor.capture());
        AnalyticsWsMessage message = messageCaptor.getValue();

        assertThat(message.type()).isEqualTo("analytics.snapshot");
        assertThat(message.manufactureId()).isEqualTo(manufactureId);
        assertThat(message.analytics()).isEqualTo(analytics);
        assertThat(message.generatedAt()).isNotNull();
    }

    @Test
    void scheduleSnapshotCancelsPreviousPendingPublishForSameManufactureId() {
        UUID manufactureId = UUID.randomUUID();
        ScheduledFuture<?> firstFuture = org.mockito.Mockito.mock(ScheduledFuture.class);
        ScheduledFuture<?> secondFuture = org.mockito.Mockito.mock(ScheduledFuture.class);

        when(analyticsWebSocketHandler.hasSubscribers(manufactureId)).thenReturn(true);
        doReturn(firstFuture, secondFuture)
                .when(analyticsWebSocketTaskScheduler)
                .schedule(any(Runnable.class), any(Instant.class));

        publisher.scheduleSnapshot(manufactureId);
        publisher.scheduleSnapshot(manufactureId);

        verify(firstFuture).cancel(false);
        verify(secondFuture, never()).cancel(false);
    }

    @Test
    void scheduledRunnableSkipsBroadcastWhenUseCaseFails() {
        UUID manufactureId = UUID.randomUUID();
        ScheduledFuture<?> future = org.mockito.Mockito.mock(ScheduledFuture.class);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        when(analyticsWebSocketHandler.hasSubscribers(manufactureId)).thenReturn(true);
        when(analyticsUseCase.getAnalyticShowDTO(manufactureId)).thenThrow(new IllegalStateException("boom"));
        doReturn(future)
                .when(analyticsWebSocketTaskScheduler)
                .schedule(runnableCaptor.capture(), any(Instant.class));

        publisher.scheduleSnapshot(manufactureId);
        runnableCaptor.getValue().run();

        verify(analyticsWebSocketHandler, never()).broadcast(any());
    }
}
