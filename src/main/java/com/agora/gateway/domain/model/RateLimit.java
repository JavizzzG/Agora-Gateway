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

    // Authentication routes: strict limit to reduce brute-force attempts.
    public static final int AUTH_REQUESTS_PER_MINUTE = 10;

    // Regular API routes: broader limit for normal usage.
    public static final int API_REQUESTS_PER_MINUTE = 50;

    // Bucket capacity to allow short bursts.
    public static final int AUTH_BURST_CAPACITY = 10;
    public static final int API_BURST_CAPACITY = 200;

    /**
     * Decides whether a route should use authentication limits.
     */
    public static boolean isAuthRoute(String path) {
        return path.startsWith("/public/auth/");
    }
}
