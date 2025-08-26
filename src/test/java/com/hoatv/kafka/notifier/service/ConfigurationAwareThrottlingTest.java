package com.hoatv.kafka.notifier.service;

import com.hoatv.kafka.notifier.model.NotifierConfiguration;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the configuration-aware throttling functionality
 */
@DisplayName("Configuration-Aware Throttling Tests")
class ConfigurationAwareThrottlingTest {

    private NotificationThrottlingService throttlingService;

    @BeforeEach
    void setUp() {
        // Create RateLimiterRegistry with defaults matching resilience4j.yml
        RateLimiterConfig defaultConfig = RateLimiterConfig.custom()
                .limitForPeriod(1)
                .limitRefreshPeriod(Duration.ofMinutes(5))
                .timeoutDuration(Duration.ofSeconds(0))
                .build();
                
        RateLimiterRegistry registry = RateLimiterRegistry.of(defaultConfig);
        throttlingService = new NotificationThrottlingService(registry);
    }

    @Test
    @DisplayName("Should use default configuration from resilience4j.yml when no custom config provided")
    void shouldUseDefaultConfigurationWhenNoCustomConfig() {
        // Create config without throttling settings (null values)
        NotifierConfiguration config = NotifierConfiguration.builder()
                .notifier("cpu-alert")
                .throttlePeriodMinutes(null)  // Use default
                .throttlePermitsPerPeriod(null)  // Use default
                .build();

        // First notification should be allowed
        assertTrue(throttlingService.shouldSendNotification(config), 
                "First notification should be allowed with default config");

        // Second immediate notification should be throttled
        assertFalse(throttlingService.shouldSendNotification(config), 
                "Second immediate notification should be throttled with default config");
    }

    @Test
    @DisplayName("Should use custom configuration when provided in NotifierConfiguration")
    void shouldUseCustomConfigurationWhenProvided() {
        // Create config with custom throttling: 2 notifications per 10 minutes
        NotifierConfiguration config = NotifierConfiguration.builder()
                .notifier("memory-alert")
                .throttlePeriodMinutes(10L)  // Custom: 10 minutes
                .throttlePermitsPerPeriod(2)  // Custom: 2 permits
                .build();

        // First notification should be allowed
        assertTrue(throttlingService.shouldSendNotification(config), 
                "First notification should be allowed with custom config");

        // Second notification should also be allowed (2 permits)
        assertTrue(throttlingService.shouldSendNotification(config), 
                "Second notification should be allowed with custom config (2 permits)");

        // Third notification should be throttled
        assertFalse(throttlingService.shouldSendNotification(config), 
                "Third notification should be throttled with custom config");
    }

    @Test
    @DisplayName("Should use partial custom configuration with defaults for missing values")
    void shouldUsePartialCustomConfiguration() {
        // Create config with only custom period, permits should use default
        NotifierConfiguration configCustomPeriod = NotifierConfiguration.builder()
                .notifier("disk-alert")
                .throttlePeriodMinutes(1L)  // Custom: 1 minute (faster for testing)
                .throttlePermitsPerPeriod(null)  // Use default: 1 permit
                .build();

        // First notification should be allowed
        assertTrue(throttlingService.shouldSendNotification(configCustomPeriod), 
                "First notification should be allowed");

        // Second notification should be throttled (default 1 permit)
        assertFalse(throttlingService.shouldSendNotification(configCustomPeriod), 
                "Second notification should be throttled with default permit count");

        // Create config with only custom permits, period should use default
        NotifierConfiguration configCustomPermits = NotifierConfiguration.builder()
                .notifier("network-alert")
                .throttlePeriodMinutes(null)  // Use default: 5 minutes
                .throttlePermitsPerPeriod(3)  // Custom: 3 permits
                .build();

        // Should allow 3 notifications
        for (int i = 1; i <= 3; i++) {
            assertTrue(throttlingService.shouldSendNotification(configCustomPermits), 
                    "Notification " + i + " should be allowed with 3 custom permits");
        }

        // Fourth notification should be throttled
        assertFalse(throttlingService.shouldSendNotification(configCustomPermits), 
                "Fourth notification should be throttled after 3 permits");
    }

    @Test
    @DisplayName("Should handle different notifiers independently")
    void shouldHandleDifferentNotifiersIndependently() {
        NotifierConfiguration cpuConfig = NotifierConfiguration.builder()
                .notifier("cpu-alert")
                .throttlePeriodMinutes(5L)
                .throttlePermitsPerPeriod(1)
                .build();

        NotifierConfiguration memoryConfig = NotifierConfiguration.builder()
                .notifier("memory-alert")
                .throttlePeriodMinutes(5L)
                .throttlePermitsPerPeriod(2)
                .build();

        // CPU alert: 1 permit
        assertTrue(throttlingService.shouldSendNotification(cpuConfig), 
                "CPU alert first notification should be allowed");
        assertFalse(throttlingService.shouldSendNotification(cpuConfig), 
                "CPU alert second notification should be throttled");

        // Memory alert: 2 permits (independent of CPU alert)
        assertTrue(throttlingService.shouldSendNotification(memoryConfig), 
                "Memory alert first notification should be allowed");
        assertTrue(throttlingService.shouldSendNotification(memoryConfig), 
                "Memory alert second notification should be allowed");
        assertFalse(throttlingService.shouldSendNotification(memoryConfig), 
                "Memory alert third notification should be throttled");
    }

    @Test
    @DisplayName("Should demonstrate configuration priority: NotifierConfiguration > resilience4j.yml defaults")
    void shouldDemonstrateConfigurationPriority() {
        // Default configuration (from resilience4j.yml)
        NotifierConfiguration defaultConfig = NotifierConfiguration.builder()
                .notifier("default-notifier")
                .throttlePeriodMinutes(null)  // Will use default: 5 minutes
                .throttlePermitsPerPeriod(null)  // Will use default: 1 permit
                .build();

        // Custom configuration (overrides defaults)
        NotifierConfiguration customConfig = NotifierConfiguration.builder()
                .notifier("custom-notifier")
                .throttlePeriodMinutes(1L)  // Override: 1 minute
                .throttlePermitsPerPeriod(3)  // Override: 3 permits
                .build();

        // Test default config - 1 permit per 5 minutes
        assertTrue(throttlingService.shouldSendNotification(defaultConfig));
        assertFalse(throttlingService.shouldSendNotification(defaultConfig));

        // Test custom config - 3 permits per 1 minute
        for (int i = 1; i <= 3; i++) {
            assertTrue(throttlingService.shouldSendNotification(customConfig));
        }
        assertFalse(throttlingService.shouldSendNotification(customConfig));
    }
}
