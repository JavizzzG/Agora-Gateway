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
 * Configura los clientes de Redis que usa el gateway.
 *
 * RedisTemplate — para operaciones síncronas (rate limiting)
 * ReactiveRedisTemplate — disponible si lo necesitas en filtros reactivos
 */
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate configurado para keys String y values Long.
     * Lo usa RedisRateLimiter para incrementar contadores.
     */
    @Bean
    public RedisTemplate<String, Long> redisTemplate(
            org.springframework.data.redis.connection.RedisConnectionFactory factory) {

        RedisTemplate<String, Long> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Keys como String — "ratelimit:ip:190.24.18.3:auth"
        template.setKeySerializer(new StringRedisSerializer());

        // Values como Long — el contador de requests
        template.setValueSerializer(new GenericToStringSerializer<>(Long.class));

        return template;
    }

    /**
     * ReactiveRedisTemplate — para uso futuro en contextos reactivos.
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
     * RedisTemplate para cachear metadatos de tokens válidos.
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
