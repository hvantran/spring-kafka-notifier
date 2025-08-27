package com.hoatv.kafka.notifier.service;

import com.hoatv.kafka.notifier.config.Resilience4jConfig;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test to verify that RateLimiterRegistry uses configuration from resilience4j.yml.
 */
@SpringBootTest(classes = {Resilience4jConfig.class})
@TestPropertySource(properties = {
    "resilience4j.ratelimiter.configs.default.limitForPeriod=1",
    "resilience4j.ratelimiter.configs.default.limitRefreshPeriod=5m",
    "resilience4j.ratelimiter.configs.default.timeoutDuration=0s"
})
class RateLimiterConfigurationTest {

    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    private NotificationThrottlingService throttlingService;

    @BeforeEach
    void setUp() {
        throttlingService = new NotificationThrottlingService(rateLimiterRegistry);
    }

    @Test
    void testDefaultConfigurationFromYml() {
        // Given: RateLimiterRegistry configured from resilience4j.yml
        assertNotNull(rateLimiterRegistry);
        
        // When: Get default configuration
        RateLimiterConfig defaultConfig = rateLimiterRegistry.getDefaultConfig();
        
        // Then: Configuration matches resilience4j.yml values
        assertEquals(1, defaultConfig.getLimitForPeriod(), "Default limit for period should be 1");
        assertEquals(Duration.ofMinutes(5), defaultConfig.getLimitRefreshPeriod(), "Default refresh period should be 5 minutes");
        assertEquals(Duration.ofSeconds(0), defaultConfig.getTimeoutDuration(), "Default timeout should be 0 seconds");
    }

    @Test
    void testThrottlingServiceUsesDefaultConfiguration() {
        // Given: NotificationThrottlingService with configured registry
        // When: First notification should be allowed
        boolean firstAllowed = throttlingService.shouldSendNotification("test-notifier");
        
        // Then: First notification is allowed
        assertEquals(true, firstAllowed, "First notification should be allowed");
        
        // When: Second notification should be throttled (limit is 1 per 5 minutes)
        boolean secondAllowed = throttlingService.shouldSendNotification("test-notifier");
        
        // Then: Second notification is throttled
        assertEquals(false, secondAllowed, "Second notification should be throttled within the same period");
    }
}
