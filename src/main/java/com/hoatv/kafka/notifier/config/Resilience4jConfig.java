package com.hoatv.kafka.notifier.config;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for Resilience4j Rate Limiter.
 * This ensures that the RateLimiterRegistry uses the configuration from resilience4j.yml.
 */
@Configuration
@EnableConfigurationProperties(Resilience4jConfig.RateLimiterProperties.class)
@Slf4j
public class Resilience4jConfig {

    /**
     * Configuration properties class that maps to resilience4j.yml structure.
     */
    @ConfigurationProperties(prefix = "resilience4j.ratelimiter.configs.default")
    @Data
    public static class RateLimiterProperties {
        private int limitForPeriod = 1;
        private Duration limitRefreshPeriod = Duration.ofMinutes(5);
        private Duration timeoutDuration = Duration.ofSeconds(0);
        private boolean registerHealthIndicator = true;
        private int eventConsumerBufferSize = 100;
    }

    /**
     * Create RateLimiterRegistry bean with default configuration from resilience4j.yml.
     * 
     * @param properties the configuration properties loaded from resilience4j.yml
     * @return configured RateLimiterRegistry
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry(RateLimiterProperties properties) {
        // Create default configuration from properties loaded from resilience4j.yml
        RateLimiterConfig defaultConfig = RateLimiterConfig.custom()
                .limitForPeriod(properties.getLimitForPeriod())
                .limitRefreshPeriod(properties.getLimitRefreshPeriod())
                .timeoutDuration(properties.getTimeoutDuration())
                .build();
        
        RateLimiterRegistry registry = RateLimiterRegistry.of(defaultConfig);
        
        log.info("RateLimiterRegistry configured with defaults from resilience4j.yml: {} permits per {} (timeout: {})", 
                defaultConfig.getLimitForPeriod(), 
                defaultConfig.getLimitRefreshPeriod(),
                defaultConfig.getTimeoutDuration());
        
        return registry;
    }
}
