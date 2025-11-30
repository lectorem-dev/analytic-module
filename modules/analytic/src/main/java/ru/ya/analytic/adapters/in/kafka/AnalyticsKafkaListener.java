package ru.ya.analytic.adapters.in.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.ya.analytic.application.in.LoadDataUseCase;
import ru.ya.libs.model.ReferedEvent;
import ru.ya.libs.model.RequestedEvent;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsKafkaListener {

    private final ObjectMapper objectMapper;
    private final LoadDataUseCase loadDataUseCase;

    @EventListener(ContextRefreshedEvent.class)
    public void startListeners() {
        log.info("AnalyticsKafkaListener is ready to receive messages.");
    }

    @KafkaListener(
            topics = "${kafka.topic.requested}",
            groupId = "analytics-service"
    )
    public void onRequestedEvent(Map<String, Object> message) {
        log.info("[Listener] Received message on 'market-requested': {}", message);

        try {
            RequestedEvent event = objectMapper.convertValue(message, RequestedEvent.class);
            loadDataUseCase.loadRequested(event);
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
            loadDataUseCase.loadReferred(event);
        } catch (Exception e) {
            log.error("Error processing ReferedEvent: {}", message, e);
        }
    }
}
