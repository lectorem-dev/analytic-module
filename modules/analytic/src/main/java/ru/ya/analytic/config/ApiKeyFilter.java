package ru.ya.analytic.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class ApiKeyFilter implements WebFilter {

    @Value("${api.key}")
    private String apiKey;

    private static final String HEADER_NAME = "X-API-KEY";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestKey = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);

        if (apiKey.equals(requestKey)) {
            return chain.filter(exchange);
        } else {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}