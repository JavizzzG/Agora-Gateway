package com.agora.gateway.infrastructure.http;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Fallback endpoints used by circuit breakers.
 *
 * When a downstream circuit is OPEN, the gateway forwards the request
 * here instead of calling the failing service.
 *
 * Clients receive an immediate and explicit 503 response.
 */
@RestController
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    @RequestMapping("/fallback/public/auth")
    public Mono<ResponseEntity<Map<String, Object>>> authFallback(
            ServerWebExchange exchange) {
        return buildFallbackResponse(
                exchange,
                "Authentication service is currently unavailable.",
                "AUTH_SERVICE_UNAVAILABLE"
        );
    }

    @RequestMapping("/fallback/users")
    public Mono<ResponseEntity<Map<String, Object>>> usersFallback(
            ServerWebExchange exchange) {
        return buildFallbackResponse(
                exchange,
                "User service is currently unavailable.",
                "USER_SERVICE_UNAVAILABLE"
        );
    }

    @RequestMapping("/fallback/workspaces")
    public Mono<ResponseEntity<Map<String, Object>>> workspacesFallback(
            ServerWebExchange exchange) {
        return buildFallbackResponse(
                exchange,
                "Workspace service is currently unavailable.",
                "WORKSPACE_SERVICE_UNAVAILABLE"
        );
    }

    private Mono<ResponseEntity<Map<String, Object>>> buildFallbackResponse(
            ServerWebExchange exchange, String message, String code) {
        String requestId = RequestContextSupport.getRequestId(exchange);
        String path = exchange.getRequest().getURI().getPath();

        log.warn("requestId={} path={} fallbackCode={}", requestId, path, code);

        Map<String, Object> body = Map.of(
                "status", 503,
                "error", code,
                "message", message,
                "path", path,
                "requestId", requestId,
                "timestamp", Instant.now().toString(),
                "retryAfter", 30
        );

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(body));
    }
}
