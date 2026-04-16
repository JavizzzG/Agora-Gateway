package com.agora.gateway.infrastructure.http;

import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

public final class RequestContextSupport {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_ATTR = "requestId";

    private RequestContextSupport() {
    }

    public static String getRequestId(ServerWebExchange exchange) {
        Object attrValue = exchange.getAttribute(REQUEST_ID_ATTR);
        if (attrValue instanceof String requestId && StringUtils.hasText(requestId)) {
            return requestId;
        }

        String headerValue = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        return StringUtils.hasText(headerValue) ? headerValue : "n/a";
    }

    public static String resolveClientIp(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String forwardedFor = headers.getFirst("X-Forwarded-For");

        if (StringUtils.hasText(forwardedFor)) {
            String firstIp = forwardedFor.split(",")[0].trim();
            if (StringUtils.hasText(firstIp)) {
                return firstIp;
            }
        }

        String realIp = headers.getFirst("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp;
        }

        if (exchange.getRequest().getRemoteAddress() != null
                && exchange.getRequest().getRemoteAddress().getAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }
}
