package com.icastar.platform.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.timeout:2000}")
    private long timeout;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        log.info("Configuring Redis connection to {}:{}", redisHost, redisPort);

        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);

        if (redisPassword != null && !redisPassword.isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(timeout))
                .build();

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serializer for values
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        log.info("Initializing Redis Cache Manager with multiple TTL configurations");

        // Default cache configuration (1 hour)
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(objectMapper())))
                .disableCachingNullValues();

        // Custom TTL configurations for different cache types
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 24 HOUR CACHES - Static/Master Data (rarely changes)
        cacheConfigurations.put(CacheNames.ARTIST_TYPES, defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigurations.put(CacheNames.ARTIST_TYPE_FIELDS, defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigurations.put(CacheNames.SUBSCRIPTION_PLANS, defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigurations.put(CacheNames.RECRUITER_CATEGORIES, defaultConfig.entryTtl(Duration.ofHours(24)));

        // 12 HOUR CACHES - Skills and aggregated data
        cacheConfigurations.put(CacheNames.SKILLS_LIST, defaultConfig.entryTtl(Duration.ofHours(12)));

        // 4 HOUR CACHES - Profile data
        cacheConfigurations.put(CacheNames.ARTISTS_BY_TYPE, defaultConfig.entryTtl(Duration.ofHours(4)));
        cacheConfigurations.put(CacheNames.ARTISTS_FEATURED, defaultConfig.entryTtl(Duration.ofHours(4)));
        cacheConfigurations.put(CacheNames.RECRUITERS_TOP, defaultConfig.entryTtl(Duration.ofHours(4)));
        cacheConfigurations.put(CacheNames.ARTISTS_ACTIVE, defaultConfig.entryTtl(Duration.ofHours(4)));

        // 2 HOUR CACHES - Job and Casting Call listings
        cacheConfigurations.put(CacheNames.JOBS_ACTIVE, defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigurations.put(CacheNames.JOBS_FEATURED, defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigurations.put(CacheNames.CASTING_CALLS, defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigurations.put(CacheNames.CASTING_CALLS_OPEN, defaultConfig.entryTtl(Duration.ofHours(2)));

        // 1 HOUR CACHES - Individual entities and counts
        cacheConfigurations.put(CacheNames.JOBS_BY_ID, defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put(CacheNames.COUNT_STATS, defaultConfig.entryTtl(Duration.ofHours(1)));

        // 30 MINUTE CACHES - User data
        cacheConfigurations.put(CacheNames.USER_BY_EMAIL, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put(CacheNames.USER_BY_ID, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put(CacheNames.USERS_BY_ROLE, defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // 15 MINUTE CACHES - Dashboard statistics
        cacheConfigurations.put(CacheNames.DASHBOARD_STATS, defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // 10 MINUTE CACHES - User-specific dashboards and profiles
        cacheConfigurations.put(CacheNames.DASHBOARD_RECRUITER, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(CacheNames.DASHBOARD_ARTIST, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(CacheNames.ARTIST_PROFILE_BY_USER, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(CacheNames.ARTIST_PROFILE_BY_ID, defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // 5 MINUTE CACHES - Frequently changing data
        cacheConfigurations.put(CacheNames.JOB_APPLICATIONS, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put(CacheNames.AUDITIONS, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put(CacheNames.BOOKMARKS_BY_USER, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put(CacheNames.BOOKMARK_CHECK, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put(CacheNames.APPLICATION_CHECK, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put(CacheNames.NOTIFICATION_UNREAD, defaultConfig.entryTtl(Duration.ofMinutes(2)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
