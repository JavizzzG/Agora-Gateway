package com.agora.gateway.infrastructure.http;

import com.agora.gateway.domain.model.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

/**
 * Manejador global de errores del gateway.
 *
 * Intercepta CUALQUIER excepción que ocurra en el gateway
 * y la convierte en una respuesta JSON limpia y consistente.
 *
 * Sin esto, el cliente puede recibir:
 * - Stack traces de Java
 * - Páginas HTML de error de Netty
 * - Respuestas vacías sin body
 *
 * Con esto, el cliente SIEMPRE recibe un JSON con esta forma:
 * {
 *   "status": 503,
 *   "error": "SERVICE_UNAVAILABLE",
 *   "message": "El servicio no está disponible...",
 *   "path": "/users/profile",
 *   "timestamp": "2024-01-01T00:00:00Z"
 * }
 */
@Component
@Order(-1) // -1 para que corra ANTES que el manejador de errores por defecto de Spring
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalErrorHandler() {
        // Configuramos Jackson para serializar Instant como ISO-8601
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        String path = exchange.getRequest().getURI().getPath();

        // Determinamos qué tipo de error es y construimos la respuesta apropiada
        ErrorResponse errorResponse = mapExceptionToError(ex, path);

        // Logueamos el error para que quede registro en los logs del gateway
        logError(path, ex, errorResponse.getStatus());

        return writeResponse(exchange, errorResponse);
    }

    /**
     * Mapea cada tipo de excepción a un ErrorResponse apropiado.
     * Aquí está la inteligencia del manejador.
     */
    private ErrorResponse mapExceptionToError(Throwable ex, String path) {

        // Servicio destino no disponible — nadie escucha en ese puerto
        if (ex instanceof ConnectException
                || ex instanceof UnknownHostException) {
            return ErrorResponse.serviceUnavailable(path);
        }

        // Timeout — el servicio tardó demasiado
        if (ex instanceof TimeoutException
                || ex.getClass().getSimpleName().contains("Timeout")) {
            return ErrorResponse.gatewayTimeout(path);
        }

        // ResponseStatusException — Spring lanza esto para 404, 405, etc.
        if (ex instanceof ResponseStatusException rse) {
            HttpStatus status = HttpStatus.resolve(rse.getStatusCode().value());

            if (status == HttpStatus.NOT_FOUND) {
                return ErrorResponse.notFound(path);
            }

            // Para otros ResponseStatusException usamos el status que trae
            return new ErrorResponse(
                    rse.getStatusCode().value(),
                    rse.getStatusCode().toString(),
                    rse.getReason() != null ? rse.getReason() : "Error en la solicitud",
                    path
            );
        }

        // IO errors generales — problemas de red
        if (ex instanceof IOException) {
            return ErrorResponse.serviceUnavailable(path);
        }

        // Cualquier otra cosa — error interno del gateway
        return ErrorResponse.internalError(path);
    }

    /**
     * Escribe la respuesta JSON en el exchange reactivo.
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
            // Si Jackson falla (no debería), fallback a string manual
            bytes = ("{\"status\":500,\"error\":\"INTERNAL_ERROR\"," +
                    "\"message\":\"Error serializando respuesta\"}").getBytes();
        }

        var buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * Log estructurado del error.
     * Nivel WARN para errores de servicios destino, ERROR para errores internos.
     */
    private void logError(String path, Throwable ex, int status) {
        if (status >= 500) {
            System.err.printf("[GATEWAY ERROR] path=%s status=%d error=%s message=%s%n",
                    path, status, ex.getClass().getSimpleName(), ex.getMessage());
        } else {
            System.out.printf("[GATEWAY WARN] path=%s status=%d error=%s%n",
                    path, status, ex.getClass().getSimpleName());
        }
    }
}
