package ru.ya.analytic.adapters.in.websocket;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class AnalyticsHandshakeInterceptor implements HandshakeInterceptor {

    public static final String MANUFACTURE_ID_ATTRIBUTE = "manufactureId";
    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String API_KEY_PARAM = "apiKey";
    private static final String MANUFACTURE_ID_PARAM = "manufactureId";

    @Value("${api.key}")
    private String apiKey;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes
    ) {
        var params = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        String key = request.getHeaders().getFirst(API_KEY_HEADER);

        if (key == null || key.isBlank()) {
            key = params.getFirst(API_KEY_PARAM);
        }

        if (!apiKey.equals(key)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            log.warn("Rejected websocket handshake: invalid API key");
            return false;
        }

        String manufactureIdRaw = params.getFirst(MANUFACTURE_ID_PARAM);
        if (manufactureIdRaw == null || manufactureIdRaw.isBlank()) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            response.getHeaders().add(HttpHeaders.WARNING, "Missing manufactureId query parameter");
            return false;
        }

        try {
            attributes.put(MANUFACTURE_ID_ATTRIBUTE, UUID.fromString(manufactureIdRaw));
            return true;
        } catch (IllegalArgumentException ex) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            response.getHeaders().add(HttpHeaders.WARNING, "Invalid manufactureId query parameter");
            log.warn("Rejected websocket handshake: invalid manufactureId '{}'", manufactureIdRaw);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            Exception exception
    ) {
        // No-op: the subscription context is already stored in session attributes.
    }
}
