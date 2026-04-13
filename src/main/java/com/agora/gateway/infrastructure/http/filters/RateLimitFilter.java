package com.agora.gateway.infrastructure.http.filters;

import com.agora.gateway.domain.model.RateLimit;
import com.agora.gateway.domain.ports.out.RateLimiterPort;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

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

    private final RateLimiterPort rateLimiter;

    public RateLimitFilter(RateLimiterPort rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path     = exchange.getRequest().getURI().getPath();
        String clientKey = resolveClientKey(exchange);
        boolean isAuth  = RateLimit.isAuthRoute(path);

        if (!rateLimiter.isAllowed(clientKey, isAuth)) {
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
        String ip = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
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

        String body = String.format(
                "{\"status\":429,\"error\":\"TOO_MANY_REQUESTS\"," +
                        "\"message\":\"%s\",\"retryAfter\":60}",
                message
        );

        var buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Justo después del filtro JWT
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
