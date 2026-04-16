package com.agora.gateway.infrastructure.http.filters;

import com.agora.gateway.infrastructure.http.RequestContextSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class RequestTracingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestTracingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incomingRequestId = exchange.getRequest().getHeaders().getFirst(RequestContextSupport.REQUEST_ID_HEADER);
        String requestId = StringUtils.hasText(incomingRequestId) ? incomingRequestId : UUID.randomUUID().toString();
        long startNanos = System.nanoTime();

        exchange.getAttributes().put(RequestContextSupport.REQUEST_ID_ATTR, requestId);

        var mutatedRequest = exchange.getRequest().mutate()
                .header(RequestContextSupport.REQUEST_ID_HEADER, requestId)
                .build();
        var mutatedExchange = exchange.mutate().request(mutatedRequest).build();
        mutatedExchange.getResponse().getHeaders().set(RequestContextSupport.REQUEST_ID_HEADER, requestId);

        return chain.filter(mutatedExchange)
                .doFinally(signal -> {
                    HttpStatusCode statusCode = mutatedExchange.getResponse().getStatusCode();
                    int status = statusCode != null ? statusCode.value() : 200;
                    long durationMs = (System.nanoTime() - startNanos) / 1_000_000;

                    log.info("requestId={} method={} path={} status={} durationMs={}",
                            requestId,
                            mutatedExchange.getRequest().getMethod(),
                            mutatedExchange.getRequest().getURI().getPath(),
                            status,
                            durationMs);
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
