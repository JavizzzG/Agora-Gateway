package com.agora.gateway.infrastructure.http.filters;

import com.agora.gateway.domain.model.UserContext;
import com.agora.gateway.domain.ports.in.AuthenticationPort;
import com.agora.gateway.domain.ports.in.AuthenticationPort.AuthenticationException;
import com.agora.gateway.infrastructure.http.RequestContextSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class JwtFilterAdapterTest {

    @Test
    void shouldReturn401WhenAuthenticationFails() {
        AuthenticationPort authenticationPort = token -> {
            throw new AuthenticationException("Token was not found", AuthenticationException.Reason.TOKEN_MISSING);
        };

        JwtFilterAdapter filter = new JwtFilterAdapter(authenticationPort, new ObjectMapper());
        MockServerHttpRequest request = MockServerHttpRequest.get("/users/me")
                .header(RequestContextSupport.REQUEST_ID_HEADER, "req-401")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = ignored -> Mono.empty();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldBypassAuthenticationForPublicRoutes() {
        AuthenticationPort authenticationPort = token -> {
            throw new IllegalStateException("Should not be called for public route");
        };
        JwtFilterAdapter filter = new JwtFilterAdapter(authenticationPort, new ObjectMapper());

        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/public/auth/authenticate").build());
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ignored -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(chainCalled.get()).isTrue();
    }

    @Test
    void shouldAttachUserHeaderWhenAuthenticationSucceeds() {
        AuthenticationPort authenticationPort = token -> new UserContext("user-42");

        JwtFilterAdapter filter = new JwtFilterAdapter(authenticationPort, new ObjectMapper());
        MockServerHttpRequest request = MockServerHttpRequest.get("/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer sample-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            assertThat(ex.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("user-42");
            assertThat(ex.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(chainCalled.get()).isTrue();
    }
}
