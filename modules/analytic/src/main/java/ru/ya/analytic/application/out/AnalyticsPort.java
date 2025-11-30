package ru.ya.analytic.application.out;

import java.util.UUID;

public interface AnalyticsPort {
    Double getAverageRank(UUID mid);

    Long getTotalCount(UUID mid);

    Integer getReferCount(UUID mid);
}