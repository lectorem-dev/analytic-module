package ru.ya.simulator.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.ya.simulator.domain.ReferedEvent;
import ru.ya.simulator.domain.RequestedEvent;

@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.requested}")
    private String requestedTopic;

    @Value("${kafka.topic.referred}")
    private String referredTopic;

    public void sendRequested(RequestedEvent event) {
        kafkaTemplate.send(requestedTopic, event.getCid().toString(), event);
    }

    public void sendReferred(ReferedEvent event) {
        kafkaTemplate.send(referredTopic, event.getMid().toString(), event);
    }
}