package ru.ya.analytic.config;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebConfig {

    @Value("${frontend.host}")
    private String frontendHost;

    @Value("${frontend.port}")
    private String frontendPort;

    /** Список разрешённых HTTP-методов для CORS. */
    private final String[] allowedMethods = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                String allowedOrigin = "http://" + frontendHost + ":" + frontendPort;
                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigin)
                        .allowedMethods(allowedMethods)
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
