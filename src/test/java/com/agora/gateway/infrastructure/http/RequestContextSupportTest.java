package com.agora.gateway.infrastructure.http;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

class RequestContextSupportTest {

    @Test
    void shouldResolveRequestIdFromAttributeFirst() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/users/me")
                .header(RequestContextSupport.REQUEST_ID_HEADER, "header-id")
                .build());
        exchange.getAttributes().put(RequestContextSupport.REQUEST_ID_ATTR, "attr-id");

        assertThat(RequestContextSupport.getRequestId(exchange)).isEqualTo("attr-id");
    }

    @Test
    void shouldResolveClientIpFromXForwardedFor() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/users/me")
                .header("X-Forwarded-For", "190.10.10.10, 10.0.0.2")
                .build());

        assertThat(RequestContextSupport.resolveClientIp(exchange)).isEqualTo("190.10.10.10");
    }

    @Test
    void shouldReturnUnknownWhenNoIpHeadersExist() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/users/me").build());

        assertThat(RequestContextSupport.resolveClientIp(exchange)).isEqualTo("unknown");
    }
}
