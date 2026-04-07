package ru.ya.analytic.adapters.in.dto;

import java.time.Instant;
import java.util.UUID;

public record AnalyticsWsMessage(
        String type,
        UUID manufactureId,
        Instant generatedAt,
        AnalyticsResponse analytics
) {
}
