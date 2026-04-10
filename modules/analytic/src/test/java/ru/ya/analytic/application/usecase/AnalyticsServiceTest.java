package ru.ya.analytic.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ya.analytic.adapters.in.dto.AnalyticsResponse;
import ru.ya.analytic.application.out.AnalyticsPort;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AnalyticsPort analyticsPort;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void getAnalyticShowDTOReturnsAllMetricsAsStrings() {
        UUID manufactureId = UUID.randomUUID();
        when(analyticsPort.getAverageRank(manufactureId)).thenReturn(12.5);
        when(analyticsPort.getTotalCount(manufactureId)).thenReturn(480L);
        when(analyticsPort.getReferCount(manufactureId)).thenReturn(37);

        AnalyticsResponse response = analyticsService.getAnalyticShowDTO(manufactureId);

        assertThat(response.getAverageRank()).isEqualTo("12.5");
        assertThat(response.getGlobalCount()).isEqualTo("480");
        assertThat(response.getReferCount()).isEqualTo("37");
    }

    @Test
    void getAnalyticAvgDTOReturnsOnlyAverageRank() {
        UUID manufactureId = UUID.randomUUID();
        when(analyticsPort.getAverageRank(manufactureId)).thenReturn(7.25);

        AnalyticsResponse response = analyticsService.getAnalyticAvgDTO(manufactureId);

        assertThat(response.getAverageRank()).isEqualTo("7.25");
        assertThat(response.getGlobalCount()).isNull();
        assertThat(response.getReferCount()).isNull();
    }

    @Test
    void getAnalyticReferDTOReturnsOnlyReferCount() {
        UUID manufactureId = UUID.randomUUID();
        when(analyticsPort.getReferCount(manufactureId)).thenReturn(19);

        AnalyticsResponse response = analyticsService.getAnalyticReferDTO(manufactureId);

        assertThat(response.getAverageRank()).isNull();
        assertThat(response.getGlobalCount()).isNull();
        assertThat(response.getReferCount()).isEqualTo("19");
    }

    @Test
    void getAnalyticCountDTOReturnsOnlyGlobalCount() {
        UUID manufactureId = UUID.randomUUID();
        when(analyticsPort.getTotalCount(manufactureId)).thenReturn(99L);

        AnalyticsResponse response = analyticsService.getAnalyticCountDTO(manufactureId);

        assertThat(response.getAverageRank()).isNull();
        assertThat(response.getGlobalCount()).isEqualTo("99");
        assertThat(response.getReferCount()).isNull();
    }
}
