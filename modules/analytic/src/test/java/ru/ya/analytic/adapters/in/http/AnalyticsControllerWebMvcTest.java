package ru.ya.analytic.adapters.in.http;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.ya.analytic.adapters.in.dto.AnalyticsResponse;
import ru.ya.analytic.application.in.GetAnalyticsUseCase;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnalyticsController.class)
@TestPropertySource(properties = "api.key=secret")
class AnalyticsControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetAnalyticsUseCase useCase;

    @Test
    void showEndpointReturnsAnalyticsJsonForValidApiKey() throws Exception {
        UUID manufactureId = UUID.randomUUID();
        when(useCase.getAnalyticShowDTO(manufactureId)).thenReturn(
                AnalyticsResponse.builder()
                        .averageRank("3.5")
                        .globalCount("42")
                        .referCount("9")
                        .build()
        );

        mockMvc.perform(get("/api/{manufactureId}/show", manufactureId)
                        .header("X-API-KEY", "secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRank").value("3.5"))
                .andExpect(jsonPath("$.globalCount").value("42"))
                .andExpect(jsonPath("$.referCount").value("9"));

        verify(useCase).getAnalyticShowDTO(manufactureId);
    }

    @Test
    void countEndpointReturnsGlobalCountForValidApiKey() throws Exception {
        UUID manufactureId = UUID.randomUUID();
        when(useCase.getAnalyticCountDTO(manufactureId)).thenReturn(
                AnalyticsResponse.builder().globalCount("15").build()
        );

        mockMvc.perform(get("/api/{manufactureId}/count", manufactureId)
                        .header("X-API-KEY", "secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.globalCount").value("15"))
                .andExpect(jsonPath("$.averageRank").isEmpty())
                .andExpect(jsonPath("$.referCount").isEmpty());

        verify(useCase).getAnalyticCountDTO(manufactureId);
    }

    @Test
    void showEndpointReturnsForbiddenForMissingApiKey() throws Exception {
        mockMvc.perform(get("/api/{manufactureId}/show", UUID.randomUUID()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(useCase);
    }

    @Test
    void showEndpointReturnsForbiddenForInvalidApiKey() throws Exception {
        mockMvc.perform(get("/api/{manufactureId}/show", UUID.randomUUID())
                        .header("X-API-KEY", "wrong"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(useCase);
    }
}
