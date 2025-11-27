package ru.ya.simulator.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.ya.libs.model.ReferedEvent;
import ru.ya.libs.model.RequestedEvent;
import ru.ya.simulator.infrastructure.kafka.KafkaEventPublisher;

import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventGeneratorService {

    private final KafkaEventPublisher publisher;
    private final Random random = new Random();

    @Scheduled(fixedRateString = "3000") // каждую секунду
    public void generateRequestedEvent() {
        RequestedEvent event = new RequestedEvent(
                UUID.randomUUID(), // cid
                UUID.randomUUID(), // mid
                random.nextInt(5) + 1, // count
                LocalDate.now()
        );
        publisher.sendRequested(event);
        log.info("Published RequestedEvent: {}", event);
    }

    @Scheduled(fixedRateString = "3000") // каждые 1.5 секунды
    public void generateReferredEvent() {
        ReferedEvent event = new ReferedEvent(
                UUID.randomUUID(), // mid
                random.nextInt(3) + 1, // count
                LocalDate.now()
        );
        publisher.sendReferred(event);
        log.info("Published ReferedEvent: {}", event);
    }
}

