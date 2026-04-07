package ru.ya.simulator.config;

import java.util.Locale;

public enum SimulationScenario {
    STABLE(1.0, 0.03, 0.008, 0.40, 1.00, 0.12, 2.00),
    TRENDING(1.1, 0.06, 0.035, 0.70, 1.10, 0.10, 2.80),
    PROMO_BURST(1.2, 0.05, 0.015, 2.20, 1.35, 0.10, 3.00),
    CHAOS(1.35, 0.11, 0.028, 1.40, 1.25, 0.07, 3.50);

    private final double trafficMultiplier;
    private final double noiseFactor;
    private final double trendStrength;
    private final double promoProbabilityMultiplier;
    private final double promoBoostMultiplier;
    private final double meanReversionRate;
    private final double maxPopularityFactor;

    SimulationScenario(
            double trafficMultiplier,
            double noiseFactor,
            double trendStrength,
            double promoProbabilityMultiplier,
            double promoBoostMultiplier,
            double meanReversionRate,
            double maxPopularityFactor
    ) {
        this.trafficMultiplier = trafficMultiplier;
        this.noiseFactor = noiseFactor;
        this.trendStrength = trendStrength;
        this.promoProbabilityMultiplier = promoProbabilityMultiplier;
        this.promoBoostMultiplier = promoBoostMultiplier;
        this.meanReversionRate = meanReversionRate;
        this.maxPopularityFactor = maxPopularityFactor;
    }

    public double getTrafficMultiplier() {
        return trafficMultiplier;
    }

    public double getNoiseFactor() {
        return noiseFactor;
    }

    public double getTrendStrength() {
        return trendStrength;
    }

    public double getPromoProbabilityMultiplier() {
        return promoProbabilityMultiplier;
    }

    public double getPromoBoostMultiplier() {
        return promoBoostMultiplier;
    }

    public double getMeanReversionRate() {
        return meanReversionRate;
    }

    public double getMaxPopularityFactor() {
        return maxPopularityFactor;
    }

    public static SimulationScenario from(String rawValue) {
        return SimulationScenario.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
    }
}
