package com.agora.gateway.infrastructure.rateLimit;

import com.agora.gateway.domain.model.RateLimit;
import com.agora.gateway.domain.ports.out.RateLimiterPort;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiter implementation backed by Bucket4j.
 *
 * Bucket4j uses the token-bucket algorithm:
 * - Each client has a bucket with N tokens
 * - Every request consumes 1 token
 * - Tokens refill at a fixed rate per minute
 * - If no token is available, the request is rejected
 *
 * ConcurrentHashMap is used because reactive execution can involve
 * concurrent access from multiple threads.
 *
 * Limitation: counters are lost on restart.
 * For distributed production setups, use RedisRateLimiter.
 */
@Component
public class InMemoryRateLimiter implements RateLimiterPort {

    // One bucket per client key (IP or userId).
    private final ConcurrentHashMap<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> apiBuckets  = new ConcurrentHashMap<>();

    @Override
    public boolean isAllowed(String clientKey, boolean isAuthRoute) {
        Bucket bucket = isAuthRoute
                ? authBuckets.computeIfAbsent(clientKey, k -> createAuthBucket())
                : apiBuckets.computeIfAbsent(clientKey, k -> createApiBucket());

        // tryConsume(1): true if token was consumed, false if bucket is empty.
        return bucket.tryConsume(1);
    }

    /**
     * Bucket policy for authentication routes.
     */
    private Bucket createAuthBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(RateLimit.AUTH_BURST_CAPACITY)
                .refillGreedy(RateLimit.AUTH_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Bucket policy for regular API routes.
     */
    private Bucket createApiBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(RateLimit.API_BURST_CAPACITY)
                .refillGreedy(RateLimit.API_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
