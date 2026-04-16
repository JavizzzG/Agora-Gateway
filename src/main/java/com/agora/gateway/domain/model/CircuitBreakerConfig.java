package com.agora.gateway.domain.model;

/**
 * Parámetros del circuit breaker para cada servicio.
 *
 * Estos valores son decisiones de negocio:
 * - ¿Cuántos fallos toleramos antes de abrir el circuito?
 * - ¿Cuánto tiempo le damos al servicio para recuperarse?
 *
 * Están aquí en el dominio, no enterrados en application.yml
 * donde nadie los encontraría.
 */
public class CircuitBreakerConfig {

    // Porcentaje de fallos para abrir el circuito
    // 50% significa: si la mitad de los últimos requests fallan → OPEN
    public static final float FAILURE_RATE_THRESHOLD = 50.0f;

    // Tiempo que el circuito permanece abierto antes de probar de nuevo
    public static final int WAIT_DURATION_SECONDS = 30;

    // Requests que se dejan pasar en estado HALF_OPEN para probar
    public static final int PERMITTED_CALLS_IN_HALF_OPEN = 3;

    // Tamaño de la ventana de requests que se evalúan
    // Los últimos 10 requests determinan si el circuito se abre
    public static final int SLIDING_WINDOW_SIZE = 10;

    // Mínimo de requests antes de que el breaker empiece a evaluar
    // Evita que 1 fallo en arranque abra el circuito
    public static final int MINIMUM_CALLS = 5;

    // Timeout por request — si el servicio tarda más de esto, cuenta como fallo
    public static final int TIMEOUT_SECONDS = 10;
}
