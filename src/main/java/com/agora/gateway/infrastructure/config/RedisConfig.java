package com.agora.gateway.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configures Redis clients used by the gateway.
 *
 * RedisTemplate: sync operations (rate limiting, token cache)
 * ReactiveRedisTemplate: available for reactive use cases
 */
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate configured with String keys and Long values.
     * Used by RedisRateLimiter to increment counters.
     */
    @Bean
    public RedisTemplate<String, Long> redisTemplate(
            org.springframework.data.redis.connection.RedisConnectionFactory factory) {

        RedisTemplate<String, Long> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Keys as String (example: "ratelimit:auth:user:123").
        template.setKeySerializer(new StringRedisSerializer());

        // Values as Long request counters.
        template.setValueSerializer(new GenericToStringSerializer<>(Long.class));

        return template;
    }

    /**
     * ReactiveRedisTemplate for future reactive integrations.
     */
    @Bean
    public ReactiveRedisTemplate<String, Long> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory) {

        RedisSerializationContext<String, Long> context =
                RedisSerializationContext.<String, Long>newSerializationContext(
                                new StringRedisSerializer())
                        .value(new GenericToStringSerializer<>(Long.class))
                        .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    /**
     * RedisTemplate for caching metadata of validated tokens.
     */
    @Bean
    public RedisTemplate<String, String> tokenCacheRedisTemplate(
            org.springframework.data.redis.connection.RedisConnectionFactory factory) {

        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
