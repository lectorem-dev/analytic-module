package ru.ya.analytic.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaTopicChecker {

    private final KafkaAdmin kafkaAdmin;

    @Value("${app.kafka.topics}")
    private List<String> requiredTopics;

    private final long checkIntervalMs = 10_000; // 15 секунд

    @PostConstruct
    public void waitForTopics() throws InterruptedException {
        boolean topicsExist = false;

        while (!topicsExist) {
            try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
                Set<String> existingTopics = adminClient.listTopics().names().get();
                List<String> missingTopics = requiredTopics.stream()
                        .filter(t -> !existingTopics.contains(t))
                        .toList();

                if (missingTopics.isEmpty()) {
                    topicsExist = true;
                    log.info("All required Kafka topics are available: {}", requiredTopics);
                } else {
                    log.warn("Waiting for Kafka topics: {}", missingTopics);
                    Thread.sleep(checkIntervalMs);
                }
            } catch (Exception e) {
                log.error("Error checking Kafka topics, retrying in {} ms", checkIntervalMs, e);
                Thread.sleep(checkIntervalMs);
            }
        }
    }
}

