package ru.ya.simulator.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class GeneratorConfigInitializer {

    private final SimulatorProperties props;
    private final Random random = new Random();

    @Getter
    private GeneratorConfig config;

    @PostConstruct
    public void init() {
        config = new GeneratorConfig();

        // Категории
        List<UUID> cids = IntStream.range(0, props.getCategories())
                .mapToObj(i -> UUID.randomUUID())
                .toList();
        config.setCategories(cids);

        // Производители
        List<UUID> mids = IntStream.range(0, props.getManufacturers())
                .mapToObj(i -> UUID.randomUUID())
                .toList();
        config.setManufacturers(mids);

        // Извлекаем диапазон популярности
        String[] pr = props.getPopularityRange().split("-");
        int minPop = Integer.parseInt(pr[0]);
        int maxPop = Integer.parseInt(pr[1]);

        // Веса популярности производителей
        Map<UUID, Integer> popularity = new HashMap<>();
        for (UUID mid : mids) {
            popularity.put(mid, random.nextInt(maxPop - minPop + 1) + minPop);
        }
        config.setPopularityMap(popularity);

        // Привязка товаров к категориям
        Map<UUID, List<UUID>> catMapping = new HashMap<>();
        for (UUID cid : cids) {
            List<UUID> midsForCat = random.ints(props.getProductsPerCategory(), 0, mids.size())
                    .mapToObj(mids::get)
                    .toList();
            catMapping.put(cid, midsForCat);
        }
        config.setCategoryProducts(catMapping);
    }
}
