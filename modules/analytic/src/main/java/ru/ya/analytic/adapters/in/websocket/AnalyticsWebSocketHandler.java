package ru.ya.analytic.adapters.in.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.ya.analytic.adapters.in.dto.AnalyticsWsMessage;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsWebSocketHandler extends TextWebSocketHandler {

    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 64 * 1024;

    private final ObjectMapper objectMapper;

    private final ConcurrentMap<UUID, Set<WebSocketSession>> sessionsByManufactureId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> manufactureIdBySessionId = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Object manufactureIdValue = session.getAttributes().get(AnalyticsHandshakeInterceptor.MANUFACTURE_ID_ATTRIBUTE);
        if (!(manufactureIdValue instanceof UUID manufactureId)) {
            closeSilently(session, CloseStatus.BAD_DATA);
            return;
        }

        WebSocketSession safeSession = new ConcurrentWebSocketSessionDecorator(
                session,
                SEND_TIME_LIMIT_MS,
                BUFFER_SIZE_LIMIT_BYTES
        );

        sessionsByManufactureId
                .computeIfAbsent(manufactureId, ignored -> ConcurrentHashMap.newKeySet())
                .add(safeSession);
        manufactureIdBySessionId.put(session.getId(), manufactureId);

        log.info("WebSocket connected: sessionId={}, manufactureId={}", session.getId(), manufactureId);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        unregister(session);
        log.info("WebSocket disconnected: sessionId={}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, @NonNull Throwable exception) {
        log.warn("WebSocket transport error for session {}", session.getId(), exception);
        unregister(session);
        closeSilently(session, CloseStatus.SERVER_ERROR);
    }

    public boolean hasSubscribers(UUID manufactureId) {
        Set<WebSocketSession> sessions = sessionsByManufactureId.get(manufactureId);
        return sessions != null && sessions.stream().anyMatch(WebSocketSession::isOpen);
    }

    public void broadcast(AnalyticsWsMessage message) {
        Set<WebSocketSession> sessions = sessionsByManufactureId.get(message.manufactureId());
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        final String payload;
        try {
            payload = objectMapper.writeValueAsString(message);
        } catch (IOException ex) {
            log.error("Failed to serialize analytics websocket message for {}", message.manufactureId(), ex);
            return;
        }

        TextMessage textMessage = new TextMessage(payload);
        sessions.removeIf(session -> !session.isOpen());

        for (WebSocketSession session : sessions) {
            try {
                session.sendMessage(textMessage);
            } catch (IOException ex) {
                log.warn("Failed to push analytics update to session {}", session.getId(), ex);
                unregister(session);
                closeSilently(session, CloseStatus.SERVER_ERROR);
            }
        }
    }

    private void unregister(WebSocketSession session) {
        UUID manufactureId = manufactureIdBySessionId.remove(session.getId());
        if (manufactureId == null) {
            return;
        }

        sessionsByManufactureId.computeIfPresent(manufactureId, (ignored, sessions) -> {
            sessions.removeIf(existing -> existing.getId().equals(session.getId()));
            return sessions.isEmpty() ? null : sessions;
        });
    }

    private void closeSilently(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (IOException ex) {
            log.debug("Failed to close websocket session {}", session.getId(), ex);
        }
    }
}
