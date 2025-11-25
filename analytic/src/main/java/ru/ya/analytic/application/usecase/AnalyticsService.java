package ru.ya.analytic.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.ya.analytic.adapters.in.model.AnalyticsResponse;
import ru.ya.analytic.application.in.GetAnalyticsUseCase;
import ru.ya.analytic.application.out.AnalyticsPort;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService implements GetAnalyticsUseCase {

    private final AnalyticsPort analyticsPort;

    @Override
    public AnalyticsResponse getAnalyticShowDTO(UUID mid) {
        // агрегируем все метрики на сервисном уровне
        Double avg = analyticsPort.getAverageRank(mid);
        Long total = analyticsPort.getTotalCount(mid);
        Integer refer = analyticsPort.getReferCount(mid);

        return AnalyticsResponse.builder()
                .averageRank(avg.toString())
                .globalCount(total.toString())
                .referCount(refer.toString())
                .build();
    }

    @Override
    public AnalyticsResponse getAnalyticAvgDTO(UUID mid) {
        Double avg = analyticsPort.getAverageRank(mid);
        return AnalyticsResponse.builder()
                .averageRank(avg.toString())
                .build();
    }

    @Override
    public AnalyticsResponse getAnalyticReferDTO(UUID mid) {
        Integer refer = analyticsPort.getReferCount(mid);
        return AnalyticsResponse.builder()
                .referCount(refer.toString())
                .build();
    }

    @Override
    public AnalyticsResponse getAnalyticCountDTO(UUID mid) {
        Long total = analyticsPort.getTotalCount(mid);
        return AnalyticsResponse.builder()
                .globalCount(total.toString())
                .build();
    }
}