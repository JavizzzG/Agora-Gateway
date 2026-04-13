package com.agora.gateway.domain.ports.out;

/**
 * Puerto de salida — el dominio necesita saber si un cliente
 * ha superado su límite de peticiones.
 *
 * No sabe si el contador está en memoria o en Redis.
 * Solo pregunta: ¿puede pasar este cliente?
 */
public interface RateLimiterPort {

    /**
     * Verifica si el cliente puede hacer una petición más.
     *
     * @param clientKey identificador del cliente — puede ser IP, userId, o API key
     * @param isAuthRoute true si es una ruta de autenticación (límite más estricto)
     * @return true si puede pasar, false si superó el límite
     */
    boolean isAllowed(String clientKey, boolean isAuthRoute);
}
