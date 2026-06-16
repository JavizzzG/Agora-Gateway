package com.agora.gateway.domain.ports.out;

/**
 * Outbound port used by the domain to decide whether a client
 * exceeded request limits.
 *
 * The domain does not care if counting is in-memory or Redis-based.
 */
public interface RateLimiterPort {

    /**
     * Checks if a client can perform one more request.
     *
     * @param clientKey client identifier, usually IP or userId
     * @param isAuthRoute true for authentication routes (stricter limit)
     * @return true if request is allowed, false if limit is exceeded
     */
    boolean isAllowed(String clientKey, boolean isAuthRoute);
}
