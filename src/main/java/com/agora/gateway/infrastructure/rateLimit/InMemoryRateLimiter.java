package com.agora.gateway.infrastructure.rateLimit;

import com.agora.gateway.domain.model.RateLimit;
import com.agora.gateway.domain.ports.out.RateLimiterPort;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación en memoria del rate limiter usando Bucket4j.
 *
 * Bucket4j usa el algoritmo "token bucket":
 * - Cada cliente tiene un bucket con N tokens
 * - Cada petición consume 1 token
 * - Los tokens se recargan a una tasa fija por minuto
 * - Si no hay tokens, la petición se rechaza
 *
 * ConcurrentHashMap porque el gateway es reactivo y
 * múltiples threads pueden acceder al mismo tiempo.
 *
 * LIMITACIÓN: los contadores se pierden si el gateway se reinicia.
 * Para producción con múltiples instancias del gateway → usar Redis.
 */
@Component
public class InMemoryRateLimiter implements RateLimiterPort {

    // Un bucket por cliente — la key es IP o userId
    private final ConcurrentHashMap<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> apiBuckets  = new ConcurrentHashMap<>();

    @Override
    public boolean isAllowed(String clientKey, boolean isAuthRoute) {
        Bucket bucket = isAuthRoute
                ? authBuckets.computeIfAbsent(clientKey, k -> createAuthBucket())
                : apiBuckets.computeIfAbsent(clientKey, k -> createApiBucket());

        // tryConsume(1) intenta consumir 1 token
        // devuelve true si había token disponible (petición permitida)
        // devuelve false si el bucket está vacío (petición rechazada)
        return bucket.tryConsume(1);
    }

    /**
     * Bucket para rutas de autenticación.
     * Límite estricto — 10 peticiones por minuto con burst de 20.
     */
    private Bucket createAuthBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(RateLimit.AUTH_BURST_CAPACITY)
                .refillGreedy(RateLimit.AUTH_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Bucket para rutas normales de la API.
     * Límite amplio — 100 peticiones por minuto con burst de 200.
     */
    private Bucket createApiBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(RateLimit.API_BURST_CAPACITY)
                .refillGreedy(RateLimit.API_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
