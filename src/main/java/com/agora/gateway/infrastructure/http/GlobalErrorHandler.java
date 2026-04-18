package com.agora.gateway.infrastructure.http;

import com.agora.gateway.domain.model.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

/**
 * Global error handler for gateway-level exceptions.
 *
 * Intercepts any exception and converts it into a consistent JSON payload.
 *
 * Without this handler, clients may receive:
 * - Java stack traces
 * - Netty HTML error pages
 * - Empty responses
 *
 * With this handler, the client always receives JSON in this shape:
 * {
 *   "status": 503,
 *   "error": "SERVICE_UNAVAILABLE",
 *   "message": "The service is currently unavailable...",
 *   "path": "/users/profile",
 *   "timestamp": "2024-01-01T00:00:00Z"
 * }
 */
@Component
@Order(-1) // Run before Spring's default error handler.
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

    private final ObjectMapper objectMapper;

    public GlobalErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        String path = exchange.getRequest().getURI().getPath();
        String requestId = RequestContextSupport.getRequestId(exchange);

        // Identify exception type and map it to a standardized payload.
        ErrorResponse errorResponse = mapExceptionToError(ex, path, requestId);

        // Persist useful operational details in logs.
        logError(path, ex, errorResponse.getStatus(), requestId);

        return writeResponse(exchange, errorResponse);
    }

    /**
     * Maps each exception type to the appropriate ErrorResponse.
     */
    private ErrorResponse mapExceptionToError(Throwable ex, String path, String requestId) {

        // Downstream service is unavailable.
        if (ex instanceof ConnectException
                || ex instanceof UnknownHostException) {
            return ErrorResponse.serviceUnavailable(path, requestId);
        }

        // Downstream timeout.
        if (ex instanceof TimeoutException
                || ex.getClass().getSimpleName().contains("Timeout")) {
            return ErrorResponse.gatewayTimeout(path, requestId);
        }

        // Framework-level HTTP errors such as 404 or 405.
        if (ex instanceof ResponseStatusException rse) {
            HttpStatus status = HttpStatus.resolve(rse.getStatusCode().value());

            if (status == HttpStatus.NOT_FOUND) {
                return ErrorResponse.notFound(path, requestId);
            }

            // Keep original status for non-404 response status errors.
            return new ErrorResponse(
                    rse.getStatusCode().value(),
                    rse.getStatusCode().toString(),
                    rse.getReason() != null ? rse.getReason() : "Request processing error.",
                    path,
                    requestId
            );
        }

        // Generic I/O/network failures.
        if (ex instanceof IOException) {
            return ErrorResponse.serviceUnavailable(path, requestId);
        }

        // Fallback for unexpected errors.
        return ErrorResponse.internalError(path, requestId);
    }

    /**
     * Writes the JSON error payload to the reactive exchange.
     */
    private Mono<Void> writeResponse(ServerWebExchange exchange,
                                     ErrorResponse errorResponse) {
        var response = exchange.getResponse();

        response.setStatusCode(HttpStatus.valueOf(errorResponse.getStatus()));
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(errorResponse);
        } catch (Exception e) {
            // Fallback in case JSON serialization fails.
            bytes = ("{\"status\":500,\"error\":\"INTERNAL_ERROR\"," +
                    "\"message\":\"Error serializing response.\"}").getBytes(StandardCharsets.UTF_8);
        }

        var buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * Structured error logging.
     * WARN for non-critical errors, ERROR for server-side failures.
     */
    private void logError(String path, Throwable ex, int status, String requestId) {
        if (status >= 500 && status < 600) {
            log.error("requestId={} path={} status={} errorType={} message={}",
                    requestId, path, status, ex.getClass().getSimpleName(), ex.getMessage(), ex);
        } else {
            log.warn("requestId={} path={} status={} errorType={} message={}",
                    requestId, path, status, ex.getClass().getSimpleName(), ex.getMessage());
        }
    }
}
