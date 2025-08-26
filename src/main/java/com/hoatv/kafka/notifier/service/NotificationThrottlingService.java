package com.hoatv.kafka.notifier.service;

import com.hoatv.kafka.notifier.model.NotifierConfiguration;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for throttling notifications using Resilience4j RateLimiter.
 * Prevents notification flooding when conditions remain true across multiple messages.
 * 
 * Configuration priority:
 * 1. NotifierConfiguration fields (per-notifier custom settings)
 * 2. resilience4j.yml default configuration (fallback)
 */
@Service
@Slf4j
public class NotificationThrottlingService {

    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();
    private final RateLimiterRegistry rateLimiterRegistry;
    
    // Default values from resilience4j.yml configuration
    private final Duration defaultPeriod;
    private final int defaultPermits;

    @Autowired
    public NotificationThrottlingService(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;
        
        // Get default configuration from resilience4j.yml
        RateLimiterConfig defaultConfig = rateLimiterRegistry.getDefaultConfig();
        this.defaultPeriod = defaultConfig.getLimitRefreshPeriod();
        this.defaultPermits = defaultConfig.getLimitForPeriod();
        
        log.info("NotificationThrottlingService initialized with defaults: {} permits per {}", 
                defaultPermits, defaultPeriod);
    }

    /**
     * Check if a notification should be sent for the given notifier using NotifierConfiguration.
     * Falls back to resilience4j.yml defaults for missing configuration.
     *
     * @param config the NotifierConfiguration containing throttle settings
     * @return true if notification should be sent, false if throttled
     */
    public boolean shouldSendNotification(NotifierConfiguration config) {
        String notifierName = config.getNotifier();
        
        // Use configuration values or fall back to defaults
        Duration period = config.getThrottlePeriodMinutes() != null 
            ? Duration.ofMinutes(config.getThrottlePeriodMinutes())
            : defaultPeriod;
            
        int permits = config.getThrottlePermitsPerPeriod() != null 
            ? config.getThrottlePermitsPerPeriod()
            : defaultPermits;
        
        return shouldSendNotification(notifierName, period, permits);
    }

    /**
     * Check if a notification should be sent for the given notifier.
     * Uses default configuration from resilience4j.yml.
     *
     * @param notifierName the name of the notifier
     * @return true if notification should be sent, false if throttled
     */
    public boolean shouldSendNotification(String notifierName) {
        return shouldSendNotification(notifierName, defaultPeriod, defaultPermits);
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
        String rateLimiterKey = String.format("%s-%d-%s", notifierName, permitsPerPeriod, period.toString());
        
        return rateLimiters.computeIfAbsent(rateLimiterKey, key -> {
            // Create custom config if different from defaults, otherwise use registry defaults
            RateLimiter rateLimiter;
            if (!period.equals(defaultPeriod) || permitsPerPeriod != defaultPermits) {
                RateLimiterConfig customConfig = RateLimiterConfig.custom()
                        .limitRefreshPeriod(period)
                        .limitForPeriod(permitsPerPeriod)
                        .timeoutDuration(Duration.ofMillis(100)) // Don't wait, fail fast
                        .build();
                        
                rateLimiter = RateLimiter.of(rateLimiterKey, customConfig);
            } else {
                // Use the configured registry with defaults from resilience4j.yml
                rateLimiter = rateLimiterRegistry.rateLimiter(rateLimiterKey);
            }
            
            log.info("Created RateLimiter for notifier: {} with {} permits per {} (key: {})", 
                    notifierName, permitsPerPeriod, period, rateLimiterKey);
            
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