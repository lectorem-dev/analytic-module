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
    private long generateIntervalMs;
    private double clickProbability;
}
