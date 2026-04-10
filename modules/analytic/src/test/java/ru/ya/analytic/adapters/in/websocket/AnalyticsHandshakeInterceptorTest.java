package ru.ya.analytic.adapters.in.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AnalyticsHandshakeInterceptorTest {

    private AnalyticsHandshakeInterceptor interceptor;
    private WebSocketHandler webSocketHandler;

    @BeforeEach
    void setUp() {
        interceptor = new AnalyticsHandshakeInterceptor();
        ReflectionTestUtils.setField(interceptor, "apiKey", "secret");
        webSocketHandler = mock(WebSocketHandler.class);
    }

    @Test
    void beforeHandshakeAcceptsValidHeaderApiKeyAndStoresManufactureId() {
        UUID manufactureId = UUID.randomUUID();
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws/analytics");
        servletRequest.setQueryString("manufactureId=" + manufactureId);
        servletRequest.addHeader("X-API-KEY", "secret");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse),
                webSocketHandler,
                attributes
        );

        assertThat(accepted).isTrue();
        assertThat(attributes.get(AnalyticsHandshakeInterceptor.MANUFACTURE_ID_ATTRIBUTE)).isEqualTo(manufactureId);
    }

    @Test
    void beforeHandshakeAcceptsApiKeyFromQueryParameterWhenHeaderMissing() {
        UUID manufactureId = UUID.randomUUID();
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws/analytics");
        servletRequest.setQueryString("manufactureId=" + manufactureId + "&apiKey=secret");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse),
                webSocketHandler,
                attributes
        );

        assertThat(accepted).isTrue();
        assertThat(attributes.get(AnalyticsHandshakeInterceptor.MANUFACTURE_ID_ATTRIBUTE)).isEqualTo(manufactureId);
    }

    @Test
    void beforeHandshakeRejectsInvalidApiKey() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws/analytics");
        servletRequest.setQueryString("manufactureId=" + UUID.randomUUID());
        servletRequest.addHeader("X-API-KEY", "wrong");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        boolean accepted = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse),
                webSocketHandler,
                new HashMap<>()
        );

        assertThat(accepted).isFalse();
        assertThat(servletResponse.getStatus()).isEqualTo(403);
    }

    @Test
    void beforeHandshakeRejectsMissingManufactureId() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws/analytics");
        servletRequest.addHeader("X-API-KEY", "secret");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);

        boolean accepted = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                response,
                webSocketHandler,
                new HashMap<>()
        );

        assertThat(accepted).isFalse();
        assertThat(servletResponse.getStatus()).isEqualTo(400);
        assertThat(response.getHeaders().getFirst(HttpHeaders.WARNING))
                .isEqualTo("Missing manufactureId query parameter");
    }

    @Test
    void beforeHandshakeRejectsInvalidManufactureId() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws/analytics");
        servletRequest.addHeader("X-API-KEY", "secret");
        servletRequest.setQueryString("manufactureId=not-a-uuid");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);

        boolean accepted = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                response,
                webSocketHandler,
                new HashMap<>()
        );

        assertThat(accepted).isFalse();
        assertThat(servletResponse.getStatus()).isEqualTo(400);
        assertThat(response.getHeaders().getFirst(HttpHeaders.WARNING))
                .isEqualTo("Invalid manufactureId query parameter");
    }
}
