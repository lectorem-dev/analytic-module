package ru.ya.analytic.adapters.in.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ya.analytic.adapters.in.dto.AnalyticsResponse;
import ru.ya.analytic.application.in.GetAnalyticsUseCase;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Analytics API", description = "Эндпоинты для получения аналитики по товарам")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final GetAnalyticsUseCase useCase;

    @Value("${api.key}")
    private String apiKey;

    public AnalyticsController(GetAnalyticsUseCase useCase) {
        this.useCase = useCase;
    }

    private boolean isValidApiKey(String key) {
        if (!apiKey.equals(key)) {
            log.warn("Отказано в доступе: неверный API-ключ '{}'", key);
            return false;
        }
        return true;
    }

    private ResponseEntity<AnalyticsResponse> forbiddenResponse() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @GetMapping("/{manufactureId}/show")
    @Operation(summary = "Показать всю аналитику для товара", description = "Возвращает объект с averageRank, globalCount, referCount")
    public ResponseEntity<AnalyticsResponse> getAnalyticShowDTO(
            @Parameter(description = "UUID товара") @PathVariable UUID manufactureId,
            @RequestHeader(name = "X-API-KEY", required = false) String key
    ) {
        if (!isValidApiKey(key)) return forbiddenResponse();
        return ResponseEntity.ok(useCase.getAnalyticShowDTO(manufactureId));
    }

    @GetMapping("/{manufactureId}/avg")
    @Operation(summary = "Средний ранг товара", description = "Возвращает объект с averageRank")
    public ResponseEntity<AnalyticsResponse> getAnalyticAvgDTO(
            @Parameter(description = "UUID товара") @PathVariable UUID manufactureId,
            @RequestHeader(name = "X-API-KEY", required = false) String key
    ) {
        if (!isValidApiKey(key)) return forbiddenResponse();
        return ResponseEntity.ok(useCase.getAnalyticAvgDTO(manufactureId));
    }

    @GetMapping("/{manufactureId}/refer")
    @Operation(summary = "Количество рефералов товара", description = "Возвращает объект с referCount")
    public ResponseEntity<AnalyticsResponse> getAnalyticReferDTO(
            @Parameter(description = "UUID товара") @PathVariable UUID manufactureId,
            @RequestHeader(name = "X-API-KEY", required = false) String key
    ) {
        if (!isValidApiKey(key)) return forbiddenResponse();
        return ResponseEntity.ok(useCase.getAnalyticReferDTO(manufactureId));
    }

    @GetMapping("/{manufactureId}/count")
    @Operation(summary = "Общее количество показов товара", description = "Возвращает объект с globalCount")
    public ResponseEntity<AnalyticsResponse> getAnalyticCountDTO(
            @Parameter(description = "UUID товара") @PathVariable UUID manufactureId,
            @RequestHeader(name = "X-API-KEY", required = false) String key
    ) {
        if (!isValidApiKey(key)) return forbiddenResponse();
        return ResponseEntity.ok(useCase.getAnalyticCountDTO(manufactureId));
    }
}