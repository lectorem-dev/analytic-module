package ru.ya.analytic.config;

import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import ru.ya.libs.model.RequestedEvent;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    private final String bootstrapServers = "kafka:9092";

    @Bean
    public ConsumerFactory<String, RequestedEvent> requestedConsumerFactory() {
        JsonDeserializer<RequestedEvent> deserializer =
                new JsonDeserializer<>(RequestedEvent.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);

        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "analytic-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RequestedEvent> requestedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, RequestedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(requestedConsumerFactory());
        factory.setMissingTopicsFatal(false);
        return factory;
    }
}
