package ru.ya.simulator.config;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class GeneratorConfig {
    private List<UUID> categories;
    private List<UUID> manufacturers;
    private Map<UUID, List<UUID>> categoryProducts;
    private Map<UUID, Integer> popularityMap;
}