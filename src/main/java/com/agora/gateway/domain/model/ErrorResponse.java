package com.agora.gateway.domain.model;

import java.time.Instant;

/**
 * Respuesta de error estándar del gateway.
 * Todos los errores del sistema tienen esta forma — sin excepciones.
 *
 * El cliente siempre sabe qué esperar cuando algo falla.
 */
public class ErrorResponse {

    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final String requestId;
    private final Instant timestamp;

    public ErrorResponse(int status, String error, String message, String path, String requestId) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.requestId = requestId;
        this.timestamp = Instant.now();
    }

    // Métodos estáticos para construir errores comunes — más legible que el constructor
    public static ErrorResponse serviceUnavailable(String path, String requestId) {
        return new ErrorResponse(
                503,
                "SERVICE_UNAVAILABLE",
                "El servicio no está disponible en este momento. Intenta más tarde.",
                path,
                requestId
        );
    }

    public static ErrorResponse gatewayTimeout(String path, String requestId) {
        return new ErrorResponse(
                504,
                "GATEWAY_TIMEOUT",
                "El servicio tardó demasiado en responder.",
                path,
                requestId
        );
    }

    public static ErrorResponse notFound(String path, String requestId) {
        return new ErrorResponse(
                404,
                "NOT_FOUND",
                "La ruta solicitada no existe.",
                path,
                requestId
        );
    }

    public static ErrorResponse internalError(String path, String requestId) {
        return new ErrorResponse(
                500,
                "INTERNAL_ERROR",
                "Error interno del gateway.",
                path,
                requestId
        );
    }

    public int getStatus()       { return status; }
    public String getError()     { return error; }
    public String getMessage()   { return message; }
    public String getPath()      { return path; }
    public String getRequestId() { return requestId; }
    public Instant getTimestamp(){ return timestamp; }
}
