package com.agora.gateway.domain.model;

import java.time.Instant;

/**
 * Standard gateway error payload.
 * Every error response in the gateway should follow this shape.
 *
 * This keeps error handling predictable for clients.
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

    // Factory helpers for common gateway errors.
    public static ErrorResponse serviceUnavailable(String path, String requestId) {
        return new ErrorResponse(
                503,
                "SERVICE_UNAVAILABLE",
                "The service is currently unavailable. Please try again later.",
                path,
                requestId
        );
    }

    public static ErrorResponse gatewayTimeout(String path, String requestId) {
        return new ErrorResponse(
                504,
                "GATEWAY_TIMEOUT",
                "The service took too long to respond.",
                path,
                requestId
        );
    }

    public static ErrorResponse notFound(String path, String requestId) {
        return new ErrorResponse(
                404,
                "NOT_FOUND",
                "The requested route does not exist.",
                path,
                requestId
        );
    }

    public static ErrorResponse internalError(String path, String requestId) {
        return new ErrorResponse(
                500,
                "INTERNAL_ERROR",
                "Internal gateway error.",
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
