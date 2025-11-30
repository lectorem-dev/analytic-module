package ru.ya.analytic.adapters.in.http;

import org.springframework.web.bind.annotation.*;
import ru.ya.analytic.adapters.in.dto.AnalyticsResponse;
import ru.ya.analytic.application.in.GetAnalyticsUseCase;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AnalyticsController {

    private final GetAnalyticsUseCase useCase;

    public AnalyticsController(GetAnalyticsUseCase useCase) {
        this.useCase = useCase;
    }

    // Единый DTO объект
    @GetMapping("/{manufactureId}/show")
    public AnalyticsResponse getAnalyticShowDTO(
            @PathVariable(name = "manufactureId") UUID manufactureId
    ) {
        return useCase.getAnalyticShowDTO(manufactureId);
    }

    // Отдельные параметры из DTO:
    @GetMapping("/{manufactureId}/avg")
    public AnalyticsResponse getAnalyticAvgDTO(
            @PathVariable(name = "manufactureId") UUID manufactureId
    ) {
        return useCase.getAnalyticAvgDTO(manufactureId);
    }

    @GetMapping("/{manufactureId}/refer")
    public AnalyticsResponse getAnalyticReferDTO(
            @PathVariable(name = "manufactureId") UUID manufactureId
    ) {
        return useCase.getAnalyticReferDTO(manufactureId);
    }

    @GetMapping("/{manufactureId}/count")
    public AnalyticsResponse getAnalyticCount(
            @PathVariable(name = "manufactureId") UUID manufactureId
    ) {
        return useCase.getAnalyticCountDTO(manufactureId);
    }
}