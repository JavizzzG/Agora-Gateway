package com.agora.gateway.infrastructure.security;

import com.agora.gateway.domain.model.AuthToken;
import com.agora.gateway.domain.ports.in.AuthenticationPort.AuthenticationException;
import com.agora.gateway.domain.ports.out.TokenValidatorPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Caches successful JWT validations in Redis to reduce repeated signature checks.
 * Invalid or expired tokens are never cached.
 */
@Primary
@Component
public class RedisCachedTokenValidator implements TokenValidatorPort {

    private static final Logger log = LoggerFactory.getLogger(RedisCachedTokenValidator.class);

    private final TokenValidatorPort delegate;
    private final RedisTemplate<String, String> tokenCacheRedisTemplate;
    private final ObjectMapper objectMapper;
    private final boolean cacheEnabled;
    private final long maxTtlSeconds;
    private final String keyPrefix;

    public RedisCachedTokenValidator(
            @Qualifier("jjwtTokenValidator") TokenValidatorPort delegate,
            @Qualifier("tokenCacheRedisTemplate") RedisTemplate<String, String> tokenCacheRedisTemplate,
            ObjectMapper objectMapper,
            @Value("${auth.token-cache.enabled:true}") boolean cacheEnabled,
            @Value("${auth.token-cache.max-ttl-seconds:60}") long maxTtlSeconds,
            @Value("${auth.token-cache.key-prefix:auth:token:valid:}") String keyPrefix) {
        this.delegate = delegate;
        this.tokenCacheRedisTemplate = tokenCacheRedisTemplate;
        this.objectMapper = objectMapper;
        this.cacheEnabled = cacheEnabled;
        this.maxTtlSeconds = maxTtlSeconds;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public AuthToken validate(String tokenData) throws AuthenticationException {
        if (!cacheEnabled || maxTtlSeconds <= 0) {
            return delegate.validate(tokenData);
        }

        String redisKey = keyPrefix + sha256(tokenData);

        AuthToken cachedToken = readFromCache(redisKey);
        if (cachedToken != null) {
            return cachedToken;
        }

        AuthToken validatedToken = delegate.validate(tokenData);
        writeToCache(redisKey, validatedToken);
        return validatedToken;
    }

    private AuthToken readFromCache(String redisKey) {
        try {
            String rawEntry = tokenCacheRedisTemplate.opsForValue().get(redisKey);
            if (rawEntry == null || rawEntry.isBlank()) {
                return null;
            }

            CachedTokenEntry entry = objectMapper.readValue(rawEntry, CachedTokenEntry.class);
            if (entry.expiresAt() == null || Instant.now().isAfter(entry.expiresAt())) {
                tokenCacheRedisTemplate.delete(redisKey);
                return null;
            }

            return new AuthToken(entry.userId(), entry.expiresAt());
        } catch (Exception ignored) {
            // Cache failures should not block authentication flow.
            log.debug("Token cache read failed for key={}", redisKey, ignored);
            return null;
        }
    }

    private void writeToCache(String redisKey, AuthToken token) {
        try {
            long secondsToExpire = Duration.between(Instant.now(), token.getExpiresAt()).toSeconds();
            long effectiveTtl = Math.min(secondsToExpire, maxTtlSeconds);
            if (effectiveTtl <= 0) {
                return;
            }

            CachedTokenEntry entry = new CachedTokenEntry(token.getUserId(), token.getExpiresAt());
            String rawEntry = objectMapper.writeValueAsString(entry);
            tokenCacheRedisTemplate.opsForValue().set(redisKey, rawEntry, Duration.ofSeconds(effectiveTtl));
        } catch (Exception ignored) {
            // Best-effort cache writes: authentication must always continue.
            log.debug("Token cache write failed for key={}", redisKey, ignored);
        }
    }

    private String sha256(String tokenData) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(tokenData.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return Integer.toHexString(tokenData.hashCode());
        }
    }

    private record CachedTokenEntry(String userId, Instant expiresAt) {
    }
}
