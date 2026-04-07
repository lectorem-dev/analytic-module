package ru.ya.simulator.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.ya.libs.model.ReferedEvent;
import ru.ya.libs.model.RequestedEvent;
import ru.ya.simulator.infrastructure.kafka.KafkaEventPublisher;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventGeneratorService {

    private final KafkaEventPublisher publisher;
    private final SimulationStateService simulationStateService;

    @Scheduled(fixedRateString = "${simulator.generate-interval-ms}")
    public void generateEvents() {
        SimulationStateService.SimulationTickPlan tickPlan = simulationStateService.nextTick();

        for (SimulationStateService.CategoryTrafficPlan categoryPlan : tickPlan.categories()) {
            for (SimulationStateService.ProductTrafficPlan productPlan : categoryPlan.products()) {
                publisher.sendRequested(new RequestedEvent(
                        categoryPlan.categoryId(),
                        productPlan.manufactureId(),
                        productPlan.requestedCount(),
                        LocalDate.now()
                ));

                if (productPlan.referredCount() > 0) {
                    publisher.sendReferred(new ReferedEvent(
                            productPlan.manufactureId(),
                            productPlan.referredCount(),
                            LocalDate.now()
                    ));
                }
            }
        }

        if (tickPlan.tick() % 5 == 0) {
            log.info(
                    "Published simulation tick={} scenario={} categories={} promo={}",
                    tickPlan.tick(),
                    tickPlan.scenario(),
                    tickPlan.activeCategories().size(),
                    tickPlan.activePromoManufactureId()
            );
        }
    }
}
