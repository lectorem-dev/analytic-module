package ru.ya.simulator.application;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.ya.simulator.config.GeneratorConfig;
import ru.ya.simulator.config.GeneratorConfigInitializer;
import ru.ya.simulator.config.SimulationScenario;
import ru.ya.simulator.config.SimulatorProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationStateService {

    private static final int TOP_PRODUCTS_LIMIT = 10;
    private static final double MIN_PROMO_MULTIPLIER = 1.1;
    private static final double MIN_POPULARITY = 1.0;

    private final GeneratorConfigInitializer configProvider;
    private final SimulatorProperties props;
    private final Random random = new Random();

    private GeneratorConfig config;
    private Map<UUID, Double> currentPopularity = new HashMap<>();
    private Map<UUID, List<UUID>> categoriesByManufactureId = new HashMap<>();

    private SimulationScenario scenario = SimulationScenario.TRENDING;
    private long tick;
    private UUID trendingManufactureId;
    private int trendTicksRemaining;
    private ActivePromo activePromo;
    private List<UUID> lastActiveCategories = List.of();

    @PostConstruct
    public synchronized void initializeState() {
        config = configProvider.getConfig();
        scenario = resolveDefaultScenario();
        currentPopularity = new HashMap<>();
        config.getPopularityMap().forEach((manufactureId, popularity) ->
                currentPopularity.put(manufactureId, popularity.doubleValue())
        );
        categoriesByManufactureId = buildCategoriesByManufactureId(config);
        rotateTrendTarget();
    }

    public synchronized SimulationTickPlan nextTick() {
        ensureInitialized();

        tick++;
        if (trendingManufactureId == null || trendTicksRemaining <= 0) {
            rotateTrendTarget();
        }

        maybeStartAutoPromo();
        evolvePopularity();

        List<UUID> activeCategories = pickActiveCategories();
        List<CategoryTrafficPlan> categories = activeCategories.stream()
                .map(this::buildCategoryTrafficPlan)
                .filter(categoryTrafficPlan -> !categoryTrafficPlan.products().isEmpty())
                .toList();

        lastActiveCategories = activeCategories;

        SimulationTickPlan tickPlan = new SimulationTickPlan(
                tick,
                scenario,
                trendingManufactureId,
                activePromo != null ? activePromo.manufactureId() : null,
                activePromo != null ? activePromo.remainingTicks() : null,
                activeCategories,
                categories
        );

        if (tick % 5 == 0) {
            log.info(
                    "Simulation tick={} scenario={} trend={} promo={} categories={}",
                    tick,
                    scenario,
                    trendingManufactureId,
                    activePromo != null ? activePromo.manufactureId() : null,
                    activeCategories.size()
            );
        }

        finalizeTick();
        return tickPlan;
    }

    public synchronized SimulationStateSnapshot getState() {
        ensureInitialized();
        return buildStateSnapshot();
    }

    public synchronized SimulationStateSnapshot setScenario(SimulationScenario newScenario) {
        ensureInitialized();
        scenario = newScenario;

        if (trendingManufactureId == null || trendTicksRemaining <= 0) {
            rotateTrendTarget();
        }

        log.info("Simulation scenario switched to {}", scenario);
        return buildStateSnapshot();
    }

    public synchronized SimulationStateSnapshot triggerPromo(UUID manufactureId, Integer ticks, Double multiplier) {
        ensureInitialized();

        UUID target = manufactureId != null ? manufactureId : pickPromoCandidate();
        if (!currentPopularity.containsKey(target)) {
            throw new IllegalArgumentException("Unknown manufactureId: " + target);
        }

        int duration = ticks != null ? Math.max(1, ticks) : props.getPromoDurationTicks();
        double promoMultiplier = multiplier != null
                ? Math.max(MIN_PROMO_MULTIPLIER, multiplier)
                : props.getPromoBoostMultiplier() * scenario.getPromoBoostMultiplier();

        activatePromo(target, duration, promoMultiplier);
        return buildStateSnapshot();
    }

    private void ensureInitialized() {
        if (config == null) {
            initializeState();
        }
    }

    private void evolvePopularity() {
        for (Map.Entry<UUID, Integer> entry : config.getPopularityMap().entrySet()) {
            UUID manufactureId = entry.getKey();
            double basePopularity = entry.getValue();
            double current = currentPopularity.getOrDefault(manufactureId, basePopularity);

            double meanReversion = (basePopularity - current) * scenario.getMeanReversionRate();
            double noise = random.nextGaussian() * basePopularity * scenario.getNoiseFactor();
            double trendBoost = Objects.equals(manufactureId, trendingManufactureId)
                    ? basePopularity * scenario.getTrendStrength()
                    : 0.0;
            double shock = buildPopularityShock(basePopularity);

            double next = current + meanReversion + noise + trendBoost + shock;
            double maxPopularity = basePopularity * scenario.getMaxPopularityFactor();

            currentPopularity.put(manufactureId, clamp(next, MIN_POPULARITY, maxPopularity));
        }
    }

    private void maybeStartAutoPromo() {
        if (activePromo != null) {
            return;
        }

        double triggerProbability = Math.min(
                1.0,
                props.getAutoPromoProbability() * scenario.getPromoProbabilityMultiplier()
        );

        if (random.nextDouble() < triggerProbability) {
            activatePromo(
                    pickPromoCandidate(),
                    props.getPromoDurationTicks(),
                    props.getPromoBoostMultiplier() * scenario.getPromoBoostMultiplier()
            );
        }
    }

    private void activatePromo(UUID manufactureId, int durationTicks, double multiplier) {
        activePromo = new ActivePromo(manufactureId, durationTicks, multiplier);
        log.info(
                "Promo activated for manufactureId={} durationTicks={} multiplier={}",
                manufactureId,
                durationTicks,
                round(multiplier)
        );
    }

    private void rotateTrendTarget() {
        trendingManufactureId = weightedPick(
                config.getManufacturers(),
                manufactureId -> currentPopularity.getOrDefault(manufactureId, 1.0)
        );
        trendTicksRemaining = Math.max(1, props.getTrendDurationTicks());
        log.info("New trending manufactureId={}", trendingManufactureId);
    }

    private List<UUID> pickActiveCategories() {
        List<UUID> pool = new ArrayList<>(config.getCategories());
        int targetCount = Math.min(Math.max(1, props.getActiveCategoriesPerTick()), pool.size());
        List<UUID> selected = new ArrayList<>(targetCount);

        while (selected.size() < targetCount && !pool.isEmpty()) {
            UUID categoryId = weightedPick(pool, this::categoryWeight);
            selected.add(categoryId);
            pool.remove(categoryId);
        }

        return selected;
    }

    private double categoryWeight(UUID categoryId) {
        double weight = config.getCategoryDemandMap().getOrDefault(categoryId, 100);
        weight *= 0.85 + random.nextDouble() * (0.60 + scenario.getNoiseFactor());

        if (isManufactureVisibleInCategory(trendingManufactureId, categoryId)) {
            weight *= scenario == SimulationScenario.TRENDING ? 1.35 : 1.10;
        }

        if (activePromo != null && isManufactureVisibleInCategory(activePromo.manufactureId(), categoryId)) {
            weight *= 1.45;
        }

        return Math.max(1.0, weight);
    }

    private CategoryTrafficPlan buildCategoryTrafficPlan(UUID categoryId) {
        List<UUID> products = config.getCategoryProducts().getOrDefault(categoryId, List.of());
        if (products.isEmpty()) {
            return new CategoryTrafficPlan(categoryId, 0, List.of());
        }

        Map<UUID, Double> rankingScore = new HashMap<>();
        for (UUID manufactureId : products) {
            rankingScore.put(manufactureId, buildRankingScore(manufactureId));
        }

        List<UUID> rankedProducts = new ArrayList<>(products);
        rankedProducts.sort(Comparator.comparingDouble((UUID manufactureId) -> rankingScore.get(manufactureId)).reversed());

        int totalViews = computeTotalViews(categoryId);
        List<Integer> requestedDistribution = distributeTraffic(totalViews, rankedProducts.size());
        List<ProductTrafficPlan> plans = new ArrayList<>(rankedProducts.size());

        for (int index = 0; index < rankedProducts.size(); index++) {
            UUID manufactureId = rankedProducts.get(index);
            int requestedCount = requestedDistribution.get(index);
            if (requestedCount <= 0) {
                continue;
            }

            int referredCount = estimateClicks(manufactureId, requestedCount, index + 1);
            plans.add(new ProductTrafficPlan(
                    manufactureId,
                    requestedCount,
                    referredCount,
                    index + 1,
                    round(rankingScore.get(manufactureId))
            ));
        }

        return new CategoryTrafficPlan(categoryId, totalViews, plans);
    }

    private double buildRankingScore(UUID manufactureId) {
        double score = currentPopularity.getOrDefault(manufactureId, MIN_POPULARITY);

        if (Objects.equals(manufactureId, trendingManufactureId)) {
            score *= 1.0 + scenario.getTrendStrength() * 4.0;
        }

        if (activePromo != null && Objects.equals(activePromo.manufactureId(), manufactureId)) {
            score *= activePromo.multiplier();
        }

        double jitter = 1.0 + random.nextGaussian() * scenario.getNoiseFactor() * 0.35;
        return score * Math.max(0.55, jitter);
    }

    private int computeTotalViews(UUID categoryId) {
        int minTraffic = Math.min(props.getMinTrafficPerTick(), props.getMaxTrafficPerTick());
        int maxTraffic = Math.max(props.getMinTrafficPerTick(), props.getMaxTrafficPerTick());
        int baseTraffic = random.nextInt(maxTraffic - minTraffic + 1) + minTraffic;

        double demandFactor = config.getCategoryDemandMap().getOrDefault(categoryId, 100) / 100.0;
        double factor = scenario.getTrafficMultiplier() * demandFactor;
        factor *= 0.85 + random.nextDouble() * (0.55 + scenario.getNoiseFactor());

        if (activePromo != null && isManufactureVisibleInCategory(activePromo.manufactureId(), categoryId)) {
            factor *= 1.25;
        }

        if (isManufactureVisibleInCategory(trendingManufactureId, categoryId)) {
            factor *= scenario == SimulationScenario.TRENDING ? 1.18 : 1.08;
        }

        if (scenario == SimulationScenario.CHAOS && random.nextDouble() < 0.12) {
            factor *= 1.8;
        }

        factor *= applyTrafficShock();

        return Math.max(1, (int) Math.round(baseTraffic * factor));
    }

    private double buildPopularityShock(double basePopularity) {
        if (random.nextDouble() >= props.getPopularityShockProbability()) {
            return 0.0;
        }

        double amplitude = basePopularity * props.getPopularityShockMultiplier();
        return random.nextBoolean() ? amplitude : -amplitude * 0.75;
    }

    private double applyTrafficShock() {
        double roll = random.nextDouble();

        if (roll < props.getTrafficSurgeProbability()) {
            return props.getTrafficSurgeMultiplier() * (0.85 + random.nextDouble() * 0.55);
        }

        if (roll < props.getTrafficSurgeProbability() + props.getTrafficDropProbability()) {
            return props.getTrafficDropMultiplier() * (0.75 + random.nextDouble() * 0.30);
        }

        return 1.0;
    }

    private List<Integer> distributeTraffic(int totalViews, int itemCount) {
        if (itemCount <= 0) {
            return List.of();
        }

        double[] weights = new double[itemCount];
        double totalWeight = 0.0;
        for (int i = 0; i < itemCount; i++) {
            weights[i] = 1.0 / Math.pow(i + 1, 1.12);
            totalWeight += weights[i];
        }

        int[] allocations = new int[itemCount];
        Map<Integer, Double> remainders = new HashMap<>();
        int allocated = 0;

        for (int i = 0; i < itemCount; i++) {
            double raw = totalViews * weights[i] / totalWeight;
            int floor = (int) Math.floor(raw);
            allocations[i] = floor;
            allocated += floor;
            remainders.put(i, raw - floor);
        }

        List<Integer> indexes = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            indexes.add(i);
        }
        indexes.sort(Comparator.comparingDouble((Integer index) -> remainders.get(index)).reversed());

        int remainder = totalViews - allocated;
        for (int i = 0; i < remainder; i++) {
            allocations[indexes.get(i % indexes.size())]++;
        }

        return Arrays.stream(allocations).boxed().toList();
    }

    private int estimateClicks(UUID manufactureId, int requestedCount, int rank) {
        if (requestedCount <= 0) {
            return 0;
        }

        double conversionBias = config.getConversionBiasMap().getOrDefault(manufactureId, 1.0);
        double ctr = props.getClickProbability() * conversionBias / Math.pow(rank, 0.82);

        if (Objects.equals(manufactureId, trendingManufactureId)) {
            ctr *= 1.05;
        }

        if (activePromo != null && Objects.equals(activePromo.manufactureId(), manufactureId)) {
            ctr *= 1.20;
        }

        if (scenario == SimulationScenario.CHAOS) {
            ctr *= 0.85 + random.nextDouble() * 0.50;
        }

        ctr = clamp(ctr, 0.0, 0.75);

        double expectedClicks = requestedCount * ctr;
        double noisyClicks = expectedClicks + random.nextGaussian() * Math.sqrt(Math.max(1.0, expectedClicks)) * 0.35;
        return (int) clamp(Math.round(noisyClicks), 0, requestedCount);
    }

    private void finalizeTick() {
        trendTicksRemaining--;

        if (activePromo == null) {
            return;
        }

        activePromo = activePromo.withRemainingTicks(activePromo.remainingTicks() - 1);
        if (activePromo.remainingTicks() <= 0) {
            log.info("Promo finished for manufactureId={}", activePromo.manufactureId());
            activePromo = null;
        }
    }

    private boolean isManufactureVisibleInCategory(UUID manufactureId, UUID categoryId) {
        if (manufactureId == null) {
            return false;
        }

        return categoriesByManufactureId.getOrDefault(manufactureId, List.of()).contains(categoryId);
    }

    private UUID pickPromoCandidate() {
        return weightedPick(
                config.getManufacturers(),
                manufactureId -> currentPopularity.getOrDefault(manufactureId, 1.0)
                        * config.getConversionBiasMap().getOrDefault(manufactureId, 1.0)
        );
    }

    private <T> T weightedPick(List<T> candidates, ToDoubleFunction<T> weightProvider) {
        double totalWeight = 0.0;
        for (T candidate : candidates) {
            totalWeight += Math.max(0.0, weightProvider.applyAsDouble(candidate));
        }

        if (totalWeight <= 0.0) {
            return candidates.get(random.nextInt(candidates.size()));
        }

        double value = random.nextDouble() * totalWeight;
        for (T candidate : candidates) {
            value -= Math.max(0.0, weightProvider.applyAsDouble(candidate));
            if (value <= 0.0) {
                return candidate;
            }
        }

        return candidates.get(candidates.size() - 1);
    }

    private Map<UUID, List<UUID>> buildCategoriesByManufactureId(GeneratorConfig generatorConfig) {
        Map<UUID, List<UUID>> result = new HashMap<>();

        for (Map.Entry<UUID, List<UUID>> entry : generatorConfig.getCategoryProducts().entrySet()) {
            UUID categoryId = entry.getKey();
            for (UUID manufactureId : entry.getValue()) {
                result.computeIfAbsent(manufactureId, ignored -> new ArrayList<>()).add(categoryId);
            }
        }

        result.replaceAll((manufactureId, categoryIds) -> List.copyOf(categoryIds));
        return result;
    }

    private SimulationScenario resolveDefaultScenario() {
        String rawScenario = props.getDefaultScenario();
        if (rawScenario == null || rawScenario.isBlank()) {
            return SimulationScenario.TRENDING;
        }

        try {
            return SimulationScenario.from(rawScenario);
        } catch (IllegalArgumentException exception) {
            log.warn("Unknown default scenario '{}', fallback to TRENDING", rawScenario);
            return SimulationScenario.TRENDING;
        }
    }

    private SimulationStateSnapshot buildStateSnapshot() {
        List<TopManufacturer> topManufacturers = currentPopularity.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(TOP_PRODUCTS_LIMIT)
                .map(entry -> new TopManufacturer(
                        entry.getKey(),
                        round(entry.getValue()),
                        config.getPopularityMap().getOrDefault(entry.getKey(), 0)
                ))
                .toList();

        return new SimulationStateSnapshot(
                scenario,
                tick,
                trendingManufactureId,
                activePromo != null ? activePromo.manufactureId() : null,
                activePromo != null ? activePromo.remainingTicks() : null,
                activePromo != null ? round(activePromo.multiplier()) : null,
                lastActiveCategories,
                topManufacturers
        );
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record ActivePromo(UUID manufactureId, int remainingTicks, double multiplier) {
        private ActivePromo withRemainingTicks(int nextRemainingTicks) {
            return new ActivePromo(manufactureId, nextRemainingTicks, multiplier);
        }
    }

    public record ProductTrafficPlan(
            UUID manufactureId,
            int requestedCount,
            int referredCount,
            int rank,
            double rankingScore
    ) {
    }

    public record CategoryTrafficPlan(
            UUID categoryId,
            int totalViews,
            List<ProductTrafficPlan> products
    ) {
    }

    public record SimulationTickPlan(
            long tick,
            SimulationScenario scenario,
            UUID trendingManufactureId,
            UUID activePromoManufactureId,
            Integer activePromoRemainingTicks,
            List<UUID> activeCategories,
            List<CategoryTrafficPlan> categories
    ) {
    }

    public record TopManufacturer(
            UUID manufactureId,
            double currentPopularity,
            int basePopularity
    ) {
    }

    public record SimulationStateSnapshot(
            SimulationScenario scenario,
            long tick,
            UUID trendingManufactureId,
            UUID activePromoManufactureId,
            Integer activePromoRemainingTicks,
            Double activePromoMultiplier,
            List<UUID> lastActiveCategories,
            List<TopManufacturer> topManufacturers
    ) {
    }
}
