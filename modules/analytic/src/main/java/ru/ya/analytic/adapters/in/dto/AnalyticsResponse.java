package ru.ya.analytic.adapters.in.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "AnalyticsResponse",
        description = "Ответ с агрегированной аналитикой по товару"
)
public class AnalyticsResponse {

    @Schema(
            description = "Средняя позиция в каталоге",
            example = "4.37"
    )
    private String averageRank;

    @Schema(
            description = "Количество показов в поиске и каталоге",
            example = "1523"
    )
    private String globalCount;

    @Schema(
            description = "Количество переходов на карточку товара",
            example = "287"
    )
    private String referCount;
}
