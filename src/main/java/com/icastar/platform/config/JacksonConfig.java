package com.icastar.platform.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson configuration for proper serialization of:
 * - Java 8+ date/time types (LocalDate, LocalDateTime, etc.)
 * - Hibernate lazy-loaded proxies and associations
 *
 * This ObjectMapper is used globally by Spring MVC and can be injected
 * into other components like Redis serializers.
 */
@Slf4j
@Configuration
public class JacksonConfig {

    /**
     * Primary ObjectMapper bean used by Spring MVC for REST API serialization.
     * Does NOT include type info - used for clean JSON responses.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        log.info("Configuring primary ObjectMapper with Hibernate6Module and JavaTimeModule");

        ObjectMapper mapper = new ObjectMapper();

        // Register Hibernate 6 module for handling lazy-loaded proxies
        Hibernate6Module hibernate6Module = new Hibernate6Module();
        // FORCE_LAZY_LOADING is disabled by default - uninitialized proxies serialize as null
        // This prevents accidental N+1 queries during serialization
        hibernate6Module.disable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
        // Serialize identifier for lazy-not-loaded objects
        hibernate6Module.enable(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS);
        mapper.registerModule(hibernate6Module);

        // Register Java 8 date/time module
        mapper.registerModule(new JavaTimeModule());

        // Serialization settings
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // Deserialization settings
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return mapper;
    }

    /**
     * ObjectMapper configured for Redis serialization.
     * Includes type information for proper deserialization from cache.
     * This is a separate instance to avoid polluting REST responses with type metadata.
     */
    @Bean(name = "redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        log.info("Configuring Redis ObjectMapper with type info, Hibernate6Module, and JavaTimeModule");

        ObjectMapper mapper = new ObjectMapper();

        // Register Hibernate 6 module
        Hibernate6Module hibernate6Module = new Hibernate6Module();
        hibernate6Module.disable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
        hibernate6Module.enable(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS);
        mapper.registerModule(hibernate6Module);

        // Register Java 8 date/time module
        mapper.registerModule(new JavaTimeModule());

        // Visibility settings for Redis (need to serialize all fields)
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // Enable type information for Redis deserialization
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // Serialization settings
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // Deserialization settings
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return mapper;
    }
}