package ru.ya.simulator.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.ya.libs.model.ReferedEvent;
import ru.ya.libs.model.RequestedEvent;
import ru.ya.simulator.config.GeneratorConfig;
import ru.ya.simulator.config.GeneratorConfigInitializer;
import ru.ya.simulator.config.SimulatorProperties;
import ru.ya.simulator.infrastructure.kafka.KafkaEventPublisher;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventGeneratorService {

    private final KafkaEventPublisher publisher;
    private final GeneratorConfigInitializer configProvider;
    private final SimulatorProperties props;
    private final Random random = new Random();

    @Scheduled(fixedRateString = "${simulator.generate-interval-ms}")
    public void generateEvents() {
        GeneratorConfig cfg = configProvider.getConfig();

        // Случайная категория
        UUID cid = cfg.getCategories().get(random.nextInt(cfg.getCategories().size()));
        List<UUID> mids = cfg.getCategoryProducts().get(cid);

        // Сортируем по популярности (имитация выдачи)
        List<UUID> ranked = mids.stream()
                .sorted(Comparator.comparingInt(m -> -cfg.getPopularityMap().get(m)))
                .toList();

        // Генерируем события просмотра
        for (UUID mid : ranked) {
            publisher.sendRequested(new RequestedEvent(
                    cid,
                    mid,
                    1,
                    LocalDate.now()
            ));
        }

        // Генерируем события перехода (клик)
        if (random.nextDouble() < props.getClickProbability()) {
            UUID clicked = ranked.get(random.nextInt(ranked.size()));
            publisher.sendReferred(new ReferedEvent(
                    clicked,
                    1,
                    LocalDate.now()
            ));
        }
    }
}
