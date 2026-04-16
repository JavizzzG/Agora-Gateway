package com.agora.gateway.infrastructure.http;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Controlador de fallback para los circuit breakers.
 *
 * Cuando el circuito de un servicio está OPEN, el gateway
 * redirige internamente aquí en lugar de intentar contactar
 * al servicio caído.
 *
 * El cliente recibe una respuesta 503 clara e inmediata
 * en lugar de esperar un timeout de 30 segundos.
 */
@RestController
public class FallbackController {

    @RequestMapping("/fallback/public/auth")
    public Mono<ResponseEntity<Map<String, Object>>> authFallback(
            ServerWebExchange exchange) {
        return buildFallbackResponse(
                "El servicio de autenticación no está disponible.",
                "AUTH_SERVICE_UNAVAILABLE"
        );
    }

    @RequestMapping("/fallback/users")
    public Mono<ResponseEntity<Map<String, Object>>> usersFallback(
            ServerWebExchange exchange) {
        return buildFallbackResponse(
                "El servicio de usuarios no está disponible.",
                "USER_SERVICE_UNAVAILABLE"
        );
    }

    @RequestMapping("/fallback/workspaces")
    public Mono<ResponseEntity<Map<String, Object>>> workspacesFallback(
            ServerWebExchange exchange) {
        return buildFallbackResponse(
                "El servicio de workspaces no está disponible.",
                "WORKSPACE_SERVICE_UNAVAILABLE"
        );
    }

    private Mono<ResponseEntity<Map<String, Object>>> buildFallbackResponse(
            String message, String code) {

        Map<String, Object> body = Map.of(
                "status", 503,
                "error", code,
                "message", message,
                "timestamp", Instant.now().toString(),
                "retryAfter", 30
        );

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(body));
    }
}
