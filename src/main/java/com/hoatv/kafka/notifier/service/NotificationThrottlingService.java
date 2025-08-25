package com.hoatv.kafka.notifier.service;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for throttling notifications using Resilience4j RateLimiter.
 * Prevents notification flooding when conditions remain true across multiple messages.
 */
@Service
@Slf4j
public class NotificationThrottlingService {

    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    /**
     * Check if a notification should be sent for the given notifier.
     * Uses RateLimiter to throttle notifications per notifier.
     *
     * @param notifierName the name of the notifier
     * @return true if notification should be sent, false if throttled
     */
    public boolean shouldSendNotification(String notifierName) {
        return shouldSendNotification(notifierName, Duration.ofMinutes(5), 1);
    }

    /**
     * Check if a notification should be sent for the given notifier with custom throttling.
     *
     * @param notifierName the name of the notifier
     * @param period the time period for the rate limit
     * @param permitsPerPeriod number of permits allowed per period
     * @return true if notification should be sent, false if throttled
     */
    public boolean shouldSendNotification(String notifierName, Duration period, int permitsPerPeriod) {
        RateLimiter rateLimiter = getRateLimiterForNotifier(notifierName, period, permitsPerPeriod);
        
        boolean allowed = rateLimiter.acquirePermission();
        
        if (!allowed) {
            log.warn("Notification throttled for notifier: {} (rate limit: {} permits per {})", 
                    notifierName, permitsPerPeriod, period);
        } else {
            log.debug("Notification allowed for notifier: {}", notifierName);
        }
        
        return allowed;
    }

    /**
     * Get or create a RateLimiter for the specified notifier.
     *
     * @param notifierName the name of the notifier
     * @param period the time period for the rate limit
     * @param permitsPerPeriod number of permits allowed per period
     * @return RateLimiter for the notifier
     */
    private RateLimiter getRateLimiterForNotifier(String notifierName, Duration period, int permitsPerPeriod) {
        return rateLimiters.computeIfAbsent(notifierName, key -> {
            RateLimiterConfig config = RateLimiterConfig.custom()
                    .limitRefreshPeriod(period)
                    .limitForPeriod(permitsPerPeriod)
                    .timeoutDuration(Duration.ofMillis(100)) // Don't wait, fail fast
                    .build();
            
            RateLimiterRegistry registry = RateLimiterRegistry.of(config);
            RateLimiter rateLimiter = registry.rateLimiter(notifierName);
            
            log.info("Created RateLimiter for notifier: {} with {} permits per {}", 
                    notifierName, permitsPerPeriod, period);
            
            return rateLimiter;
        });
    }
    
    /**
     * Clear the rate limiter for a specific notifier (useful for testing or reset scenarios).
     *
     * @param notifierName the name of the notifier
     */
    public void clearRateLimiter(String notifierName) {
        rateLimiters.remove(notifierName);
        log.info("Cleared RateLimiter for notifier: {}", notifierName);
    }
    
    /**
     * Clear all rate limiters.
     */
    public void clearAllRateLimiters() {
        rateLimiters.clear();
        log.info("Cleared all RateLimiters");
    }
}