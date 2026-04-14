package com.agora.gateway.infrastructure.rateLimit;

import com.agora.gateway.domain.model.RateLimit;
import com.agora.gateway.domain.ports.out.RateLimiterPort;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Implementación del rate limiter usando Redis.
 *
 * A diferencia de InMemoryRateLimiter:
 * - Los contadores son compartidos entre todas las instancias del gateway
 * - Sobreviven reinicios del gateway
 * - Redis expira las keys automáticamente con TTL
 *
 * Se registra con un nombre explícito para no chocar con el bean
 * redisRateLimiter que Spring Cloud Gateway auto-configura internamente.
 *
 * @Primary hace que Spring inyecte este en lugar de InMemoryRateLimiter
 * cuando ambos están en el classpath.
 */
@Primary
@Component("gatewayRedisRateLimiter")
public class RedisRateLimiter implements RateLimiterPort {

    private final RedisTemplate<String, Long> redisTemplate;

    public RedisRateLimiter(RedisTemplate<String, Long> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isAllowed(String clientKey, boolean isAuthRoute) {
        int limit = isAuthRoute
                ? RateLimit.AUTH_REQUESTS_PER_MINUTE
                : RateLimit.API_REQUESTS_PER_MINUTE;

        String redisKey = "ratelimit:" + clientKey;

        try {
            // Incrementa el contador atómicamente
            // Si la key no existe, Redis la crea con valor 0 y luego incrementa a 1
            Long count = redisTemplate.opsForValue().increment(redisKey);

            // En el primer request de esta ventana, establecemos el TTL
            // Después de 60 segundos Redis borra la key automáticamente
            // y el contador vuelve a cero
            if (count != null && count == 1) {
                redisTemplate.expire(redisKey, Duration.ofMinutes(1));
            }

            return count != null && count <= limit;

        } catch (Exception e) {
            // Si Redis no está disponible, permitimos el tráfico
            // Es mejor tener el sistema funcionando sin rate limiting
            // que tumbar el gateway porque Redis cayó
            System.err.printf(
                    "[GATEWAY WARN] Redis no disponible para rate limiting: %s. " +
                            "Permitiendo request.%n", e.getMessage()
            );
            return true;
        }
    }
}
