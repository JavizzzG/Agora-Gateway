package com.agora.gateway.infrastructure.http.filters;

import com.agora.gateway.domain.ports.out.RateLimiterPort;
import com.agora.gateway.infrastructure.http.RequestContextSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    @Test
    void shouldReturn429WhenClientExceededRateLimit() {
        RateLimiterPort rateLimiter = (clientKey, isAuthRoute) -> false;

        RateLimitFilter filter = new RateLimitFilter(rateLimiter, new ObjectMapper());

        MockServerHttpRequest request = MockServerHttpRequest.get("/users/me")
                .header(RequestContextSupport.REQUEST_ID_HEADER, "req-123")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ignored -> Mono.empty();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void shouldContinueChainWhenClientIsAllowed() {
        RateLimiterPort rateLimiter = (clientKey, isAuthRoute) -> true;

        RateLimitFilter filter = new RateLimitFilter(rateLimiter, new ObjectMapper());

        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/users/me").build());
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ignored -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(chainCalled.get()).isTrue();
    }
}
