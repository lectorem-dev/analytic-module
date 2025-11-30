package ru.ya.simulator.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.ya.libs.model.ReferedEvent;
import ru.ya.libs.model.RequestedEvent;

@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.requested}")
    private String requestedTopic;

    @Value("${kafka.topic.referred}")
    private String referredTopic;

    public void sendRequested(RequestedEvent event) {
        kafkaTemplate.send(requestedTopic, event.getCategoryId().toString(), event);
    }

    public void sendReferred(ReferedEvent event) {
        kafkaTemplate.send(referredTopic, event.getManufactureId().toString(), event);
    }
}