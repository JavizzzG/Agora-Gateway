package com.agora.gateway.infrastructure.http.filters;

import com.agora.gateway.domain.model.RateLimit;
import com.agora.gateway.domain.ports.out.RateLimiterPort;
import com.agora.gateway.infrastructure.http.RequestContextSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Filtro de rate limiting — corre después del filtro JWT.
 *
 * Orden de filtros:
 * 1. JwtFilterAdapter   (HIGHEST_PRECEDENCE = -2147483648)
 * 2. RateLimitFilter    (HIGHEST_PRECEDENCE + 1 = -2147483647)
 *
 * Primero validamos identidad, luego aplicamos el límite.
 * Así el límite es por usuario autenticado cuando hay token,
 * o por IP cuando no hay token (rutas públicas).
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimiterPort rateLimiter;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimiterPort rateLimiter, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path     = exchange.getRequest().getURI().getPath();
        String clientKey = resolveClientKey(exchange);
        boolean isAuth  = RateLimit.isAuthRoute(path);

        if (!rateLimiter.isAllowed(clientKey, isAuth)) {
            log.warn("requestId={} path={} clientKey={} rateLimited=true",
                    RequestContextSupport.getRequestId(exchange),
                    path,
                    clientKey);
            return buildRateLimitResponse(exchange, isAuth);
        }

        return chain.filter(exchange);
    }

    /**
     * Determina la clave del cliente para el rate limiting.
     *
     * Si el request ya pasó por el filtro JWT y tiene X-User-Id,
     * usamos el userId — el límite es por usuario, no por IP.
     * Esto es más justo: varios usuarios detrás del mismo proxy
     * no comparten el mismo límite.
     *
     * Si no hay userId (ruta pública), usamos la IP.
     */
    private String resolveClientKey(ServerWebExchange exchange) {
        // Intentamos usar el userId que JwtFilterAdapter agregó
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (userId != null && !userId.isBlank()) {
            return "user:" + userId;
        }

        // Fallback a IP — para rutas públicas como /auth/authenticate
        String ip = RequestContextSupport.resolveClientIp(exchange);
        return "ip:" + ip;
    }

    private Mono<Void> buildRateLimitResponse(ServerWebExchange exchange,
                                              boolean isAuth) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Header estándar que le dice al cliente cuándo puede reintentar
        response.getHeaders().add("Retry-After", "60");

        String message = isAuth
                ? "Demasiados intentos de autenticación. Espera 1 minuto."
                : "Demasiadas peticiones. Espera 1 minuto.";

        String body = buildJsonBody(exchange, message);

        var buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private String buildJsonBody(ServerWebExchange exchange, String message) {
        Map<String, Object> body = Map.of(
                "status", 429,
                "error", "TOO_MANY_REQUESTS",
                "message", message,
                "retryAfter", 60,
                "path", exchange.getRequest().getURI().getPath(),
                "requestId", RequestContextSupport.getRequestId(exchange)
        );

        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            return "{\"status\":429,\"error\":\"TOO_MANY_REQUESTS\",\"message\":\"Rate limit exceeded\"}";
        }
    }

    @Override
    public int getOrder() {
        // Justo después del filtro JWT
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
