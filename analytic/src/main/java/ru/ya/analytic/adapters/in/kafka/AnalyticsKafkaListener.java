package ru.ya.analytic.adapters.in.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
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

    // Listener запускается только после того, как контекст готов
    @EventListener(ContextRefreshedEvent.class)
    public void startListeners() {
        log.info("AnalyticsKafkaListener is ready to receive messages.");
    }

    @KafkaListener(topics = "${kafka.topic.requested}", groupId = "analytics-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void onRequestedEvent(Object message) {
        log.info("[Listener] Received message on 'market-requested': {}", message);
        try {
            RequestedEvent event = objectMapper.convertValue(message, RequestedEvent.class);
            loadDataUseCase.loadRequested(List.of(event));
        } catch (Exception e) {
            log.error("Error processing RequestedEvent: {}", message, e);
        }
    }

    @KafkaListener(topics = "${kafka.topic.referred}", groupId = "analytics-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void onReferredEvent(Object message) {
        log.info("[Listener] Received message on 'market-referred': {}", message);
        try {
            ReferedEvent event = objectMapper.convertValue(message, ReferedEvent.class);
            loadDataUseCase.loadReferred(List.of(event));
        } catch (Exception e) {
            log.error("Error processing ReferedEvent: {}", message, e);
        }
    }
}
