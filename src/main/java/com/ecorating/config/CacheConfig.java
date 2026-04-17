package com.ecorating.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper redisObjectMapper = new ObjectMapper();
        redisObjectMapper.registerModule(new JavaTimeModule());
        redisObjectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper);
        RedisSerializationContext.SerializationPair<Object> valueSerialization =
                RedisSerializationContext.SerializationPair.fromSerializer(serializer);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(valueSerialization)
                .computePrefixWith(cacheName -> "ecorating::" + cacheName + "::")
                .entryTtl(Duration.ofHours(1));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("air-quality", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigs.put("green-zones", defaultConfig.entryTtl(Duration.ofHours(6)));
        cacheConfigs.put("hazard-index", defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigs.put("mos-wifi-dataset", defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigs.put("mos-cinema-dataset-v3", defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigs.put("mos-landscaping-dataset-v3", defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigs.put("nominatim-geocode", defaultConfig.entryTtl(Duration.ofDays(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
