package com.agora.gateway.domain.model;

/**
 * Define los límites de peticiones por tipo de ruta.
 *
 * Estos valores son decisiones de negocio:
 * - ¿Cuántos intentos de login permite el sistema por minuto?
 * - ¿Cuántas peticiones normales puede hacer un usuario?
 *
 * Están aquí en el dominio, no enterrados en un filtro de infraestructura.
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
     * Determina qué límite aplica según la ruta.
     */
    public static boolean isAuthRoute(String path) {
        return path.startsWith("/public/auth/");
    }
}
