package ru.ya.analytic.adapters.in.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {
    private String averageRank; // Средняя позиция в каталоге
    private String globalCount; // Показы в поиске и каталоге (попадал в выборку)
    private String referCount;  // Переходы на карточки
}
