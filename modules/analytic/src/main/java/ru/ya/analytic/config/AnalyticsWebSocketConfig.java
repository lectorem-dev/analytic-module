package ru.ya.analytic.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import ru.ya.analytic.adapters.in.websocket.AnalyticsHandshakeInterceptor;
import ru.ya.analytic.adapters.in.websocket.AnalyticsWebSocketHandler;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class AnalyticsWebSocketConfig implements WebSocketConfigurer {

    private final AnalyticsWebSocketHandler analyticsWebSocketHandler;
    private final AnalyticsHandshakeInterceptor analyticsHandshakeInterceptor;

    @Value("${frontend.host}")
    private String frontendHost;

    @Value("${frontend.port}")
    private String frontendPort;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String allowedOrigin = "http://" + frontendHost + ":" + frontendPort;

        registry.addHandler(analyticsWebSocketHandler, "/ws/analytics")
                .addInterceptors(analyticsHandshakeInterceptor)
                .setAllowedOrigins(allowedOrigin);
    }

    @Bean
    public ThreadPoolTaskScheduler analyticsWebSocketTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("analytics-ws-");
        scheduler.initialize();
        return scheduler;
    }
}
