package com.agora.gateway.domain.model;

/**
 * Circuit breaker defaults shared by downstream services.
 *
 * These are reliability decisions:
 * - How many failures are tolerated before opening the circuit?
 * - How long should the service have to recover?
 *
 * Keeping them in the domain layer makes them visible and intentional.
 */
public class CircuitBreakerConfig {

    // Failure rate percentage needed to open the circuit.
    public static final float FAILURE_RATE_THRESHOLD = 50.0f;

    // How long the circuit stays open before switching to half-open.
    public static final int WAIT_DURATION_SECONDS = 30;

    // Number of trial calls allowed while in HALF_OPEN state.
    public static final int PERMITTED_CALLS_IN_HALF_OPEN = 3;

    // Sliding window size used for failure-rate calculation.
    public static final int SLIDING_WINDOW_SIZE = 10;

    // Minimum calls before breaker metrics are considered stable.
    public static final int MINIMUM_CALLS = 5;

    // Per-request timeout considered as a failure.
    public static final int TIMEOUT_SECONDS = 10;
}
