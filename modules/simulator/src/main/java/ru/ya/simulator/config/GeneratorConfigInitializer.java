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

        List<UUID> cids = IntStream.range(0, props.getCategories())
                .mapToObj(i -> UUID.randomUUID())
                .toList();
        config.setCategories(cids);

        List<UUID> mids = IntStream.range(0, props.getManufacturers())
                .mapToObj(i -> UUID.randomUUID())
                .toList();
        config.setManufacturers(mids);

        String[] pr = props.getPopularityRange().split("-");
        int minPop = Integer.parseInt(pr[0]);
        int maxPop = Integer.parseInt(pr[1]);

        Map<UUID, Integer> popularity = new HashMap<>();
        Map<UUID, Double> conversionBias = new HashMap<>();
        for (UUID mid : mids) {
            popularity.put(mid, random.nextInt(maxPop - minPop + 1) + minPop);
            conversionBias.put(mid, 0.70 + random.nextDouble() * 0.75);
        }
        config.setPopularityMap(popularity);
        config.setConversionBiasMap(conversionBias);

        Map<UUID, List<UUID>> catMapping = new HashMap<>();
        Map<UUID, Integer> categoryDemand = new HashMap<>();
        int productsPerCategory = Math.min(props.getProductsPerCategory(), mids.size());

        for (UUID cid : cids) {
            List<UUID> shuffled = new ArrayList<>(mids);
            Collections.shuffle(shuffled, random);
            List<UUID> midsForCat = List.copyOf(shuffled.subList(0, productsPerCategory));
            catMapping.put(cid, midsForCat);
            categoryDemand.put(cid, 80 + random.nextInt(81));
        }
        config.setCategoryProducts(catMapping);
        config.setCategoryDemandMap(categoryDemand);
    }
}
