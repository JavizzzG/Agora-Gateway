package com.agora.gateway.infrastructure.rateLimit;

import com.agora.gateway.domain.model.RateLimit;
import com.agora.gateway.domain.ports.out.RateLimiterPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis-backed rate limiter implementation.
 *
 * Compared with InMemoryRateLimiter:
 * - Counters are shared across all gateway instances
 * - Counters survive process restarts
 * - Redis handles key expiration via TTL
 *
 * Uses an explicit bean name to avoid clashes with Spring Cloud Gateway's
 * own internal redisRateLimiter bean.
 *
 * @Primary ensures this implementation is injected by default.
 */
@Primary
@Component("gatewayRedisRateLimiter")
public class RedisRateLimiter implements RateLimiterPort {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    private final RedisTemplate<String, Long> redisTemplate;

    public RedisRateLimiter(RedisTemplate<String, Long> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isAllowed(String clientKey, boolean isAuthRoute) {
        int limit = isAuthRoute
                ? RateLimit.AUTH_REQUESTS_PER_MINUTE
                : RateLimit.API_REQUESTS_PER_MINUTE;

        String routeGroup = isAuthRoute ? "auth" : "api";
        String redisKey = "ratelimit:" + routeGroup + ":" + clientKey;

        try {
            // Atomically increment per-window request counter.
            Long count = redisTemplate.opsForValue().increment(redisKey);

            // On first hit, set one-minute TTL for fixed-window behavior.
            if (count != null && count == 1) {
                redisTemplate.expire(redisKey, Duration.ofMinutes(1));
            }

            return count != null && count <= limit;

        } catch (Exception e) {
            // Fail-open strategy: do not block traffic when Redis is unavailable.
            log.warn("redisRateLimitUnavailable key={} message={} allowingRequest=true", redisKey, e.getMessage(), e);
            return true;
        }
    }
}
