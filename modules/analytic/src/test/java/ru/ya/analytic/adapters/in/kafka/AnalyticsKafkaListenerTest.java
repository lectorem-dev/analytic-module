package ru.ya.analytic.adapters.in.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ya.analytic.application.in.LoadDataUseCase;
import ru.ya.analytic.application.usecase.AnalyticsWebSocketPublisher;
import ru.ya.libs.model.ReferedEvent;
import ru.ya.libs.model.RequestedEvent;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsKafkaListenerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private LoadDataUseCase loadDataUseCase;

    @Mock
    private AnalyticsWebSocketPublisher analyticsWebSocketPublisher;

    @InjectMocks
    private AnalyticsKafkaListener listener;

    @Test
    void onRequestedEventSchedulesSnapshotAfterSuccessfulLoad() {
        UUID manufactureId = UUID.randomUUID();
        RequestedEvent event = new RequestedEvent(UUID.randomUUID(), manufactureId, 12, LocalDate.now());
        Map<String, Object> message = Map.of("manufactureId", manufactureId.toString());

        when(objectMapper.convertValue(message, RequestedEvent.class)).thenReturn(event);
        when(loadDataUseCase.loadRequested(event)).thenReturn(true);

        listener.onRequestedEvent(message);

        verify(loadDataUseCase).loadRequested(event);
        verify(analyticsWebSocketPublisher).scheduleSnapshot(manufactureId);
    }

    @Test
    void onRequestedEventDoesNotScheduleSnapshotWhenLoadFails() {
        UUID manufactureId = UUID.randomUUID();
        RequestedEvent event = new RequestedEvent(UUID.randomUUID(), manufactureId, 12, LocalDate.now());
        Map<String, Object> message = Map.of("manufactureId", manufactureId.toString());

        when(objectMapper.convertValue(message, RequestedEvent.class)).thenReturn(event);
        when(loadDataUseCase.loadRequested(event)).thenReturn(false);

        listener.onRequestedEvent(message);

        verify(loadDataUseCase).loadRequested(event);
        verify(analyticsWebSocketPublisher, never()).scheduleSnapshot(manufactureId);
    }

    @Test
    void onRequestedEventSwallowsMappingErrors() {
        Map<String, Object> message = Map.of("manufactureId", UUID.randomUUID().toString());
        when(objectMapper.convertValue(message, RequestedEvent.class))
                .thenThrow(new IllegalArgumentException("bad payload"));

        listener.onRequestedEvent(message);

        verifyNoInteractions(loadDataUseCase, analyticsWebSocketPublisher);
    }

    @Test
    void onReferredEventSchedulesSnapshotAfterSuccessfulLoad() {
        UUID manufactureId = UUID.randomUUID();
        ReferedEvent event = new ReferedEvent(manufactureId, 5, LocalDate.now());
        Map<String, Object> message = Map.of("manufactureId", manufactureId.toString());

        when(objectMapper.convertValue(message, ReferedEvent.class)).thenReturn(event);
        when(loadDataUseCase.loadReferred(event)).thenReturn(true);

        listener.onReferredEvent(message);

        verify(loadDataUseCase).loadReferred(event);
        verify(analyticsWebSocketPublisher).scheduleSnapshot(manufactureId);
    }

    @Test
    void onReferredEventDoesNotScheduleSnapshotWhenLoadFails() {
        UUID manufactureId = UUID.randomUUID();
        ReferedEvent event = new ReferedEvent(manufactureId, 5, LocalDate.now());
        Map<String, Object> message = Map.of("manufactureId", manufactureId.toString());

        when(objectMapper.convertValue(message, ReferedEvent.class)).thenReturn(event);
        when(loadDataUseCase.loadReferred(event)).thenReturn(false);

        listener.onReferredEvent(message);

        verify(loadDataUseCase).loadReferred(event);
        verify(analyticsWebSocketPublisher, never()).scheduleSnapshot(manufactureId);
    }

    @Test
    void onReferredEventSwallowsMappingErrors() {
        Map<String, Object> message = Map.of("manufactureId", UUID.randomUUID().toString());
        when(objectMapper.convertValue(message, ReferedEvent.class))
                .thenThrow(new IllegalArgumentException("bad payload"));

        listener.onReferredEvent(message);

        verifyNoInteractions(loadDataUseCase, analyticsWebSocketPublisher);
    }
}
