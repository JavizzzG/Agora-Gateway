package com.agora.gateway.domain.model;

/**
 * Defines request limits by route type.
 *
 * These are business-level decisions:
 * - How many login attempts are allowed per minute?
 * - How many regular API requests are allowed per minute?
 *
 * They live in the domain layer instead of being hidden in infrastructure filters.
 */
public class RateLimit {

    // Rutas de autenticación — límite estricto para prevenir fuerza bruta
    // 10 intentos por minuto es generoso para un humano, restrictivo para un bot
    public static final int AUTH_REQUESTS_PER_MINUTE = 200;

    // Rutas normales de la API — límite más amplio para uso normal
    public static final int API_REQUESTS_PER_MINUTE = 500;

    // Capacidad inicial del bucket — permite pequeñas ráfagas
    // Un usuario puede hacer 20 peticiones rápidas antes de ser limitado
    public static final int AUTH_BURST_CAPACITY = 20;
    public static final int API_BURST_CAPACITY = 500;

    /**
     * Decides whether a route should use authentication limits.
     */
    public static boolean isAuthRoute(String path) {
        return path.startsWith("/public/auth/");
    }
}
