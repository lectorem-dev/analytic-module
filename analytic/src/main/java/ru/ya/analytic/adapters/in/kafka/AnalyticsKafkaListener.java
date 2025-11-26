package ru.ya.analytic.adapters.in.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.ya.analytic.adapters.in.model.ReferedEvent;
import ru.ya.analytic.adapters.in.model.RequestedEvent;
import ru.ya.analytic.application.in.LoadDataUseCase;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsKafkaListener {

    private final ObjectMapper objectMapper;
    private final LoadDataUseCase loadDataUseCase;

    // Слушаем события показа (RequestedEvent)
    @KafkaListener(
            topics = "market-requested",
            groupId = "analytics-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onRequestedEvent(String message) {
        try {
            // Парсим одно событие
            RequestedEvent event = objectMapper.readValue(message, RequestedEvent.class);
            // Передаем в UseCase
            loadDataUseCase.loadRequested(List.of(event)); // TODO: FIX (UseCase ожидает List)
        } catch (Exception e) {
            log.error("Error processing requested event: {}", message, e);
        }
    }

    // Слушаем события перехода (ReferedEvent)
    @KafkaListener(
            topics = "market-referred",
            groupId = "analytics-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onReferredEvent(String message) {
        try {
            ReferedEvent event = objectMapper.readValue(message, ReferedEvent.class);
            loadDataUseCase.loadReferred(List.of(event)); // UseCase ожидает List
        } catch (Exception e) {
            log.error("Error processing referred event: {}", message, e);
        }
    }
}
