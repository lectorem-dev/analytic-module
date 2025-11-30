package ru.ya.simulator.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topic.requested}")
    private String requestedTopic;

    @Value("${kafka.topic.referred}")
    private String referredTopic;

    @Bean
    public NewTopic requestedTopic() {
        return TopicBuilder.name(requestedTopic)
                .partitions(3)         // количество партиций
                .replicas(1)           // количество реплик
                .build();
    }

    @Bean
    public NewTopic referredTopic() {
        return TopicBuilder.name(referredTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}

