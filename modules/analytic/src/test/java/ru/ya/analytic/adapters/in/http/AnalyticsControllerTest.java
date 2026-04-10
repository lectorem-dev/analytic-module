package ru.ya.analytic.adapters.in.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import ru.ya.analytic.adapters.in.dto.AnalyticsResponse;
import ru.ya.analytic.application.in.GetAnalyticsUseCase;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private GetAnalyticsUseCase useCase;

    private AnalyticsController controller;

    @BeforeEach
    void setUp() {
        controller = new AnalyticsController(useCase);
        ReflectionTestUtils.setField(controller, "apiKey", "secret");
    }

    @Test
    void getAnalyticShowDTOReturnsForbiddenForInvalidApiKey() {
        ResponseEntity<AnalyticsResponse> response =
                controller.getAnalyticShowDTO(UUID.randomUUID(), "wrong-key");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNull();
        verifyNoInteractions(useCase);
    }

    @Test
    void getAnalyticShowDTOReturnsPayloadForValidApiKey() {
        UUID manufactureId = UUID.randomUUID();
        AnalyticsResponse expected = AnalyticsResponse.builder()
                .averageRank("2.0")
                .globalCount("10")
                .referCount("3")
                .build();
        when(useCase.getAnalyticShowDTO(manufactureId)).thenReturn(expected);

        ResponseEntity<AnalyticsResponse> response =
                controller.getAnalyticShowDTO(manufactureId, "secret");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(useCase).getAnalyticShowDTO(manufactureId);
    }

    @Test
    void getAnalyticAvgDTOReturnsPayloadForValidApiKey() {
        UUID manufactureId = UUID.randomUUID();
        AnalyticsResponse expected = AnalyticsResponse.builder().averageRank("5.5").build();
        when(useCase.getAnalyticAvgDTO(manufactureId)).thenReturn(expected);

        ResponseEntity<AnalyticsResponse> response =
                controller.getAnalyticAvgDTO(manufactureId, "secret");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(useCase).getAnalyticAvgDTO(manufactureId);
    }

    @Test
    void getAnalyticReferDTOReturnsPayloadForValidApiKey() {
        UUID manufactureId = UUID.randomUUID();
        AnalyticsResponse expected = AnalyticsResponse.builder().referCount("8").build();
        when(useCase.getAnalyticReferDTO(manufactureId)).thenReturn(expected);

        ResponseEntity<AnalyticsResponse> response =
                controller.getAnalyticReferDTO(manufactureId, "secret");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(useCase).getAnalyticReferDTO(manufactureId);
    }

    @Test
    void getAnalyticCountDTOReturnsPayloadForValidApiKey() {
        UUID manufactureId = UUID.randomUUID();
        AnalyticsResponse expected = AnalyticsResponse.builder().globalCount("15").build();
        when(useCase.getAnalyticCountDTO(manufactureId)).thenReturn(expected);

        ResponseEntity<AnalyticsResponse> response =
                controller.getAnalyticCountDTO(manufactureId, "secret");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(useCase).getAnalyticCountDTO(manufactureId);
    }
}
