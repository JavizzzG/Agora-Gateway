package com.agora.gateway.infrastructure.http.filters;

import com.agora.gateway.domain.model.PublicRoute;
import com.agora.gateway.domain.model.UserContext;
import com.agora.gateway.domain.ports.in.AuthenticationPort;
import com.agora.gateway.domain.ports.in.AuthenticationPort.AuthenticationException;
import com.agora.gateway.infrastructure.http.RequestContextSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTTP input adapter bridging Spring Cloud Gateway and the domain layer.
 *
 * Responsibilities:
 * - Extract token from HTTP headers
 * - Delegate authentication to the use case
 * - Add authenticated user context to request headers
 * - Build a 401 response when auth fails
 *
 * This class does not:
 * - Validate JWT signatures directly
 * - Decide public vs protected routes
 * - Depend on JWT library details
 */
@Component
public class JwtFilterAdapter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtFilterAdapter.class);

    private final AuthenticationPort authenticationPort;
    private final ObjectMapper objectMapper;

    public JwtFilterAdapter(AuthenticationPort authenticationPort, ObjectMapper objectMapper) {
        this.authenticationPort = authenticationPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Public paths bypass authentication.
        if (PublicRoute.matches(path)) {
            return chain.filter(exchange);
        }

        // Extract token from Authorization header.
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        String rawToken = extractToken(authHeader);

        try {
            // Authenticate through the application use case.
            UserContext userContext = authenticationPort.authenticate(rawToken);

            // Propagate user identity and remove raw JWT from forwarded headers.
            var mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userContext.getUserId())
                    .headers(h -> h.remove(HttpHeaders.AUTHORIZATION))
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (AuthenticationException e) {
            log.warn("requestId={} path={} authFailed reason={}",
                    RequestContextSupport.getRequestId(exchange),
                    path,
                    e.getReason());
            return buildErrorResponse(exchange, e);
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }

        String prefix = "Bearer ";
        if (authHeader.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return authHeader.substring(prefix.length()).trim();
        }
        return null;
    }

    private Mono<Void> buildErrorResponse(ServerWebExchange exchange,
                                          AuthenticationException e) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = buildJsonBody(exchange, e);

        var buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private String buildJsonBody(ServerWebExchange exchange, AuthenticationException e) {
        Map<String, Object> body = Map.of(
                "status", 401,
                "error", "UNAUTHORIZED",
                "code", e.getReason().name(),
                "message", e.getMessage(),
                "path", exchange.getRequest().getURI().getPath(),
                "requestId", RequestContextSupport.getRequestId(exchange)
        );

        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            return "{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Authentication failed\"}";
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
