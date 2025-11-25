package ru.ya.analytic.application.in;

import ru.ya.analytic.adapters.in.model.AnalyticsResponse;

import java.util.UUID;

public interface GetAnalyticsUseCase {
    AnalyticsResponse getAnalyticShowDTO(UUID mid);

    AnalyticsResponse getAnalyticAvgDTO(UUID mid);

    AnalyticsResponse getAnalyticReferDTO(UUID mid);

    AnalyticsResponse getAnalyticCountDTO(UUID mid);
}
