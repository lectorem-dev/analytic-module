package ru.ya.simulator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "simulator")
public class SimulatorProperties {
    private int categories;
    private int manufacturers;
    private int productsPerCategory;
    private String popularityRange;
    private String defaultScenario;
    private long generateIntervalMs;
    private double clickProbability;
    private int activeCategoriesPerTick;
    private int minTrafficPerTick;
    private int maxTrafficPerTick;
    private int promoDurationTicks;
    private double promoBoostMultiplier;
    private double autoPromoProbability;
    private int trendDurationTicks;
    private double trafficSurgeProbability;
    private double trafficSurgeMultiplier;
    private double trafficDropProbability;
    private double trafficDropMultiplier;
    private double popularityShockProbability;
    private double popularityShockMultiplier;
}
