package com.agora.gateway.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

/**
 * Declares gateway routes in Java.
 *
 * Each route defines:
 * - id: unique name for logs and debugging
 * - path: URL pattern that triggers the route
 * - uri: downstream service destination
 */
@Configuration
public class RouteConfig {

    @Value("${services.auth-url}")
    private String authServiceUrl;

    @Value("${services.user-url}")
    private String userServiceUrl;

    @Value("${services.workspace-url}")
    private String workspaceServiceUrl;

    @Value("${services.ai-agent-url}")
    private String aiAgentUrl;

    @Value("${services.payment-url}")
    private String paymentServiceUrl;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()

                // Public auth endpoints (no JWT required).
                .route("auth-service", r -> r
                        .path("/public/auth/**", "/auth/google/callback")
                        .filters(f -> f.circuitBreaker(config -> config.setName("auth-service").setFallbackUri("forward:/fallback/public/auth")))
                        .uri(URI.create(authServiceUrl))
                )

                // Protected user endpoints (JWT required).
                .route("user-service", r -> r
                        .path("/users/**")
                        .filters(f -> f.circuitBreaker(config -> config.setName("user-service").setFallbackUri("forward:/fallback/users")))
                        .uri(URI.create(userServiceUrl))
                )

                // Protected workspace endpoints (JWT required).
                .route("workspace-service", r -> r
                        .path("/workspaces/**")
                        .filters(f -> f.circuitBreaker(config -> config.setName("workspace-service").setFallbackUri("forward:/fallback/workspaces")))
                        .uri(URI.create(workspaceServiceUrl))
                )

                // Protected payment endpoints (JWT required).
                .route("payment-service", r -> r
                        .path("/payment/**")
                        .filters(f -> f.circuitBreaker(config -> config.setName("payment-service").setFallbackUri("forward:/fallback/payment")))
                        .uri(URI.create(paymentServiceUrl))
                )


                // ── AI Agent service ───────────────────────────
                // Protegida — requiere JWT válido
                // stripPrefix(1) elimina /ai para que /ai/chat -> /chat, /ai/health -> /health
                .route("ai-agent-service", r -> r
                        .path("/ai/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .circuitBreaker(config -> config.setName("ai-agent-service").setFallbackUri("forward:/fallback/ai"))
                        )
                        .uri(URI.create(aiAgentUrl))
                )

                .build();
    }
}
