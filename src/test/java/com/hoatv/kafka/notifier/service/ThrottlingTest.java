package com.hoatv.kafka.notifier.service;

import io.github.resilience4j.ratelimiter.RateLimiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the Resilience4j RateLimiter throttling approach
 */
@DisplayName("Resilience4j Throttling Tests")
class ThrottlingTest {

    @Test
    @DisplayName("Should demonstrate RateLimiter throttling concept")
    void shouldDemonstrateRateLimiterThrottling() {
        // Create a rate limiter: 1 permission per 5 seconds
        RateLimiter rateLimiter = RateLimiter.of("test-notifier", 
            io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
                .limitForPeriod(1)                    // 1 notification
                .limitRefreshPeriod(java.time.Duration.ofSeconds(5))  // per 5 seconds
                .timeoutDuration(java.time.Duration.ZERO)             // Don't wait
                .build()
        );

        // First call should succeed
        assertTrue(rateLimiter.acquirePermission(), "First notification should be allowed");
        
        // Second immediate call should be throttled
        assertFalse(rateLimiter.acquirePermission(), "Second immediate notification should be throttled");
        
        // Third immediate call should also be throttled  
        assertFalse(rateLimiter.acquirePermission(), "Third immediate notification should be throttled");
        
        // Check metrics
        RateLimiter.Metrics metrics = rateLimiter.getMetrics();
        assertEquals(0, metrics.getAvailablePermissions(), "Should have no available permissions after being used");
        // Note: Number of waiting threads is 0 because timeout is set to ZERO (fail fast)
        
        System.out.println("Rate limiter metrics:");
        System.out.println("Available permissions: " + metrics.getAvailablePermissions());
        System.out.println("Number of waiting threads: " + metrics.getNumberOfWaitingThreads());
    }

    @Test
    @DisplayName("Should show how throttling prevents notification flooding")  
    void shouldShowHowThrottlingPreventsNotificationFlooding() {
        // Simulate the real scenario
        RateLimiter cpuAlertLimiter = RateLimiter.of("cpu-alert", 
            io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
                .limitForPeriod(1)                     // Max 1 notification
                .limitRefreshPeriod(java.time.Duration.ofMinutes(5))  // per 5 minutes  
                .timeoutDuration(java.time.Duration.ZERO)
                .build()
        );

        // Simulate multiple messages that match the rule
        int sentNotifications = 0;
        int totalMessages = 10;
        
        for (int i = 1; i <= totalMessages; i++) {
            // Simulate Kafka message: "85" (CPU usage > 80%)
            boolean rulePassed = true; // Rule evaluation: 85 > 80 = true
            
            if (rulePassed) {
                // Check if we should send notification (throttling)
                boolean shouldSend = cpuAlertLimiter.acquirePermission();
                
                if (shouldSend) {
                    sentNotifications++;
                    System.out.println("✅ Notification sent for message #" + i);
                } else {
                    System.out.println("🛑 Notification throttled for message #" + i);
                }
            }
        }
        
        // Verify only 1 notification was sent despite 10 matching messages
        assertEquals(1, sentNotifications, "Should only send 1 notification despite 10 matching messages");
        System.out.println("\n📊 Result: " + sentNotifications + " notifications sent out of " + totalMessages + " matching messages");
    }
}
