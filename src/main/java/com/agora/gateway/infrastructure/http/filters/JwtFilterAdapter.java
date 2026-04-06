package com.agora.gateway.infrastructure.http.filters;

import com.agora.gateway.domain.model.PublicRoute;
import com.agora.gateway.domain.model.UserContext;
import com.agora.gateway.domain.ports.in.AuthenticationPort;
import com.agora.gateway.domain.ports.in.AuthenticationPort.AuthenticationException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Adaptador de entrada HTTP — el puente entre Spring Cloud Gateway y el dominio.
 *
 * Responsabilidades de esta clase:
 * - Extraer el token del header HTTP
 * - Llamar al puerto de autenticación
 * - Mutar el request con los headers del usuario
 * - Construir la respuesta 401 si falla
 *
 * Lo que NO hace esta clase:
 * - Validar JWT (eso es JjwtTokenValidator)
 * - Decidir qué rutas son públicas (eso es PublicRoute)
 * - Conocer EdDSA o JJWT (eso es infraestructura de seguridad)
 */
@Component
public class JwtFilterAdapter implements GlobalFilter, Ordered {

    private final AuthenticationPort authenticationPort;

    public JwtFilterAdapter(AuthenticationPort authenticationPort) {
        this.authenticationPort = authenticationPort;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Rutas públicas — delegamos la decisión al dominio
        if (PublicRoute.matches(path)) {
            return chain.filter(exchange);
        }

        // Extraemos el token del header
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        String rawToken = extractToken(authHeader);

        try {
            // Llamamos al caso de uso — el filtro no sabe cómo funciona
            UserContext userContext = authenticationPort.authenticate(rawToken);

            // Mutamos el request: agregamos contexto del usuario, quitamos JWT crudo
            var mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userContext.getUserId())
                    .headers(h -> h.remove(HttpHeaders.AUTHORIZATION))
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (AuthenticationException e) {
            return buildErrorResponse(exchange, e);
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private Mono<Void> buildErrorResponse(ServerWebExchange exchange,
                                          AuthenticationException e) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

        String body = String.format(
                "{\"error\":\"%s\",\"code\":\"%s\",\"status\":401}",
                e.getMessage(),
                e.getReason().name()
        );

        var buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
