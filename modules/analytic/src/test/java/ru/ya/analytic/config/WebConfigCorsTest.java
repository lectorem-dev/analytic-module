package ru.ya.analytic.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.ya.analytic.adapters.in.http.AnalyticsController;
import ru.ya.analytic.application.in.GetAnalyticsUseCase;

import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnalyticsController.class)
@Import(WebConfig.class)
@TestPropertySource(properties = {
        "frontend.host=localhost",
        "frontend.port=5173",
        "api.key=secret"
})
class WebConfigCorsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetAnalyticsUseCase useCase;

    @Test
    void preflightRequestAllowsConfiguredFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/{manufactureId}/show", UUID.randomUUID())
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        verifyNoInteractions(useCase);
    }

    @Test
    void preflightRequestRejectsUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/{manufactureId}/show", UUID.randomUUID())
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(useCase);
    }
}
