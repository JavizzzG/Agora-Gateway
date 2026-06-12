package com.agora.gateway.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

/**
 * Define las rutas del gateway en código Java.
 *
 * Cada ruta tiene:
 * - id: nombre único para logs y debugging
 * - path: el patrón de URL que activa esta ruta
 * - uri: el servicio destino dentro de agora-network
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

    @Value("${services.media-url}")
    private String mediaServiceUrl;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()

                // ── Auth service ─────────────────────────────
                // Ruta pública — el JwtFilterAdapter la deja pasar
                .route("auth-service", r -> r
                        .path("/public/auth/**", "/auth/google/callback")
                        .filters(f -> f.circuitBreaker(config -> config.setName("auth-service").setFallbackUri("forward:/fallback/public/auth")))
                        .uri(URI.create(authServiceUrl))
                )

                // ── User service ──────────────────────────────
                // Protegida — requiere JWT válido
                .route("user-service", r -> r
                        .path("/users/**")
                        .filters(f -> f.circuitBreaker(config -> config.setName("user-service").setFallbackUri("forward:/fallback/users")))
                        .uri(URI.create(userServiceUrl))
                )

                // ── Workspace service ─────────────────────────
                // Protegida — requiere JWT válido
                .route("workspace-service", r -> r
                        .path("/workspaces/**")
                        .filters(f -> f.circuitBreaker(config -> config.setName("workspace-service").setFallbackUri("forward:/fallback/workspaces")))
                        .uri(URI.create(workspaceServiceUrl))
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

                // ── Media service ──────────────────────────────
                // Protegida — requiere JWT válido
                // NOTA: el media service ya incluye /media como prefix en app.include_router(router, prefix="/media")
                .route("media-service", r -> r
                        .path("/media/**")
                        .filters(f -> f.circuitBreaker(config -> config.setName("media-service").setFallbackUri("forward:/fallback/media")))
                        .uri(URI.create(mediaServiceUrl))
                )

                .build();
    }
}
