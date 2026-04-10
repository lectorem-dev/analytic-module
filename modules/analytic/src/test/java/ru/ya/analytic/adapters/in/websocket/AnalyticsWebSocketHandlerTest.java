package ru.ya.analytic.adapters.in.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import ru.ya.analytic.adapters.in.dto.AnalyticsResponse;
import ru.ya.analytic.adapters.in.dto.AnalyticsWsMessage;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsWebSocketHandlerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WebSocketSession session;

    private AnalyticsWebSocketHandler handler;
    private UUID manufactureId;

    @BeforeEach
    void setUp() {
        handler = new AnalyticsWebSocketHandler(objectMapper);
        manufactureId = UUID.randomUUID();
        lenient().when(session.getId()).thenReturn("session-1");
        lenient().when(session.isOpen()).thenReturn(true);
        lenient().when(session.getAttributes()).thenReturn(new HashMap<>(Map.of(
                AnalyticsHandshakeInterceptor.MANUFACTURE_ID_ATTRIBUTE,
                manufactureId
        )));
    }

    @Test
    void afterConnectionEstablishedRegistersSubscriber() {
        handler.afterConnectionEstablished(session);

        assertThat(handler.hasSubscribers(manufactureId)).isTrue();
    }

    @Test
    void afterConnectionEstablishedClosesSessionWhenManufactureIdMissing() throws IOException {
        when(session.getAttributes()).thenReturn(new HashMap<>());

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.BAD_DATA);
        assertThat(handler.hasSubscribers(manufactureId)).isFalse();
    }

    @Test
    void broadcastSendsSerializedMessageToRegisteredSubscriber() throws Exception {
        handler.afterConnectionEstablished(session);
        AnalyticsWsMessage message = new AnalyticsWsMessage(
                "analytics.snapshot",
                manufactureId,
                Instant.now(),
                AnalyticsResponse.builder().averageRank("1.0").globalCount("5").referCount("2").build()
        );
        when(objectMapper.writeValueAsString(message)).thenReturn("{\"type\":\"analytics.snapshot\"}");

        handler.broadcast(message);

        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getPayload()).isEqualTo("{\"type\":\"analytics.snapshot\"}");
    }

    @Test
    void broadcastSkipsSendingWhenSerializationFails() throws Exception {
        handler.afterConnectionEstablished(session);
        AnalyticsWsMessage message = new AnalyticsWsMessage(
                "analytics.snapshot",
                manufactureId,
                Instant.now(),
                AnalyticsResponse.builder().globalCount("5").build()
        );
        when(objectMapper.writeValueAsString(message)).thenThrow(new JsonProcessingException("boom") { });

        handler.broadcast(message);

        verify(session, never()).sendMessage(org.mockito.ArgumentMatchers.any(TextMessage.class));
    }

    @Test
    void afterConnectionClosedUnregistersSubscriber() {
        handler.afterConnectionEstablished(session);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertThat(handler.hasSubscribers(manufactureId)).isFalse();
    }

    @Test
    void handleTransportErrorUnregistersSubscriberAndClosesSession() throws IOException {
        handler.afterConnectionEstablished(session);

        handler.handleTransportError(session, new IOException("boom"));

        assertThat(handler.hasSubscribers(manufactureId)).isFalse();
        verify(session).close(CloseStatus.SERVER_ERROR);
    }
}
