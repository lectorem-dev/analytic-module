package ru.ya.analytic.adapters.in.http;

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
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final GetAnalyticsUseCase useCase;

    @Value("${api.key}")
    private String apiKey;

    public AnalyticsController(GetAnalyticsUseCase useCase) {
        this.useCase = useCase;
    }

    // Приватная функция для проверки API-ключа
    private boolean isValidApiKey(String key) {
        log.warn("Отказано в доступе: неверный API-ключ '{}'", key);
        return apiKey.equals(key);
    }

    private ResponseEntity<AnalyticsResponse> forbiddenResponse() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @GetMapping("/{manufactureId}/show")
    public ResponseEntity<AnalyticsResponse> getAnalyticShowDTO(
            @PathVariable UUID manufactureId,
            @RequestHeader(name = "X-API-KEY", required = false) String key
    ) {
        if (!isValidApiKey(key)) return forbiddenResponse();
        return ResponseEntity.ok(useCase.getAnalyticShowDTO(manufactureId));
    }

    @GetMapping("/{manufactureId}/avg")
    public ResponseEntity<AnalyticsResponse> getAnalyticAvgDTO(
            @PathVariable UUID manufactureId,
            @RequestHeader(name = "X-API-KEY", required = false) String key
    ) {
        if (!isValidApiKey(key)) return forbiddenResponse();
        return ResponseEntity.ok(useCase.getAnalyticAvgDTO(manufactureId));
    }

    @GetMapping("/{manufactureId}/refer")
    public ResponseEntity<AnalyticsResponse> getAnalyticReferDTO(
            @PathVariable UUID manufactureId,
            @RequestHeader(name = "X-API-KEY", required = false) String key
    ) {
        if (!isValidApiKey(key)) return forbiddenResponse();
        return ResponseEntity.ok(useCase.getAnalyticReferDTO(manufactureId));
    }

    @GetMapping("/{manufactureId}/count")
    public ResponseEntity<AnalyticsResponse> getAnalyticCountDTO(
            @PathVariable UUID manufactureId,
            @RequestHeader(name = "X-API-KEY", required = false) String key
    ) {
        if (!isValidApiKey(key)) return forbiddenResponse();
        return ResponseEntity.ok(useCase.getAnalyticCountDTO(manufactureId));
    }
}