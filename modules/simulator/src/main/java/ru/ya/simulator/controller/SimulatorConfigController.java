package ru.ya.simulator.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ya.simulator.config.GeneratorConfig;
import ru.ya.simulator.config.GeneratorConfigInitializer;

@RestController
@RequiredArgsConstructor
@RequestMapping("/simulator/config")
public class SimulatorConfigController {

    private final GeneratorConfigInitializer initializer;

    @GetMapping
    public GeneratorConfig getConfig() {
        return initializer.getConfig();
    }
}
