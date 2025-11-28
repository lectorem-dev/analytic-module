package ru.ya.analytic.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class KafkaTopicChecker {

    private final KafkaAdmin kafkaAdmin;

    @Value("${kafka.topic.requested}")
    private String requestedTopic;

    @Value("${kafka.topic.referred}")
    private String referredTopic;

    private final long checkIntervalMs = 5000; // 5 секунд

    @PostConstruct
    public void waitForTopics() throws InterruptedException {
        boolean topicsReady = false;

        while (!topicsReady) {
            try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
                Set<String> existingTopics = adminClient.listTopics().names().get();
                List<String> missing = List.of(requestedTopic, referredTopic).stream()
                        .filter(t -> !existingTopics.contains(t))
                        .collect(Collectors.toList());

                if (missing.isEmpty()) {
                    topicsReady = true;
                    log.info("All required Kafka topics exist: {}", List.of(requestedTopic, referredTopic));
                } else {
                    // Создаём недостающие
                    missing.forEach(t -> {
                        try {
                            adminClient.createTopics(List.of(new NewTopic(t, 1, (short)1))).all().get();
                            log.info("Created missing topic: {}", t);
                        } catch (Exception e) {
                            log.error("Failed to create topic: {}", t, e);
                        }
                    });
                    log.info("Waiting for Kafka topics to be available: {}", missing);
                    TimeUnit.MILLISECONDS.sleep(checkIntervalMs);
                }
            } catch (Exception e) {
                log.error("Error checking Kafka topics, retrying in {} ms", checkIntervalMs, e);
                TimeUnit.MILLISECONDS.sleep(checkIntervalMs);
            }
        }
    }
}
