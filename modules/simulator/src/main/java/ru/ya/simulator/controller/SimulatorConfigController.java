package ru.ya.simulator.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.ya.simulator.application.SimulationStateService;
import ru.ya.simulator.config.GeneratorConfig;
import ru.ya.simulator.config.GeneratorConfigInitializer;
import ru.ya.simulator.config.SimulationScenario;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/simulator")
public class SimulatorConfigController {

    private final GeneratorConfigInitializer initializer;
    private final SimulationStateService simulationStateService;

    @GetMapping("/config")
    public GeneratorConfig getConfig() {
        return initializer.getConfig();
    }

    @GetMapping("/state")
    public SimulationStateService.SimulationStateSnapshot getState() {
        return simulationStateService.getState();
    }

    @PostMapping("/scenario")
    public SimulationStateService.SimulationStateSnapshot updateScenario(
            @RequestParam("mode") String mode
    ) {
        try {
            return simulationStateService.setScenario(SimulationScenario.from(mode));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown scenario: " + mode, exception);
        }
    }

    @PostMapping("/promo")
    public SimulationStateService.SimulationStateSnapshot triggerPromo(
            @RequestParam(name = "manufactureId", required = false) UUID manufactureId,
            @RequestParam(name = "ticks", required = false) Integer ticks,
            @RequestParam(name = "multiplier", required = false) Double multiplier
    ) {
        try {
            return simulationStateService.triggerPromo(manufactureId, ticks, multiplier);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}
