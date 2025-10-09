package com.hoatv.kafka.notifier.service;

import com.hoatv.kafka.notifier.model.NotifierConfiguration;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.hoatv.fwk.common.ultilities.StringCommonUtils.removeInvalidUserData;

/**
 * Service for throttling notifications using Resilience4j RateLimiter.
 * Prevents notification flooding when conditions remain true across multiple messages.
 * <p>
 * Configuration priority:
 * 1. NotifierConfiguration fields (per-notifier custom settings)
 * 2. resilience4j.yml default configuration (fallback)
 */
@Service
public class NotificationThrottlingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationThrottlingService.class);

    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();
    private final RateLimiterRegistry rateLimiterRegistry;
    private final Duration defaultPeriod;
    private final int defaultPermits;

    @Autowired
    public NotificationThrottlingService(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;

        // Get default configuration from resilience4j.yml
        RateLimiterConfig defaultConfig = rateLimiterRegistry.getDefaultConfig();
        this.defaultPeriod = defaultConfig.getLimitRefreshPeriod();
        this.defaultPermits = defaultConfig.getLimitForPeriod();

        LOGGER.info("NotificationThrottlingService initialized with defaults from resilience4j.yml: {} permits per {} (timeout: {})",
                defaultPermits, defaultPeriod, defaultConfig.getTimeoutDuration());
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
     * @param notifierName     the name of the notifier
     * @param period           the time period for the rate limit
     * @param permitsPerPeriod number of permits allowed per period
     * @return true if notification should be sent, false if throttled
     */
    public boolean shouldSendNotification(String notifierName, Duration period, int permitsPerPeriod) {
        RateLimiter rateLimiter = getRateLimiterForNotifier(notifierName, period, permitsPerPeriod);

        boolean allowed = rateLimiter.acquirePermission();

        if (!allowed) {
            LOGGER.warn("Notification throttled for notifier: {} (rate limit: {} permits per {})",
                    removeInvalidUserData(notifierName), permitsPerPeriod, period);
        } else {
            LOGGER.debug("Notification allowed for notifier: {}", notifierName);
        }

        return allowed;
    }

    /**
     * Get or create a RateLimiter for the specified notifier.
     *
     * @param notifierName     the name of the notifier
     * @param period           the time period for the rate limit
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

            LOGGER.info("Created RateLimiter for notifier: {} with {} permits per {} (key: {})",
                    removeInvalidUserData(notifierName), permitsPerPeriod, period, removeInvalidUserData(rateLimiterKey));

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
        LOGGER.info("Cleared RateLimiter for notifier: {}", removeInvalidUserData(notifierName));
    }

    /**
     * Clear all rate limiters.
     */
    public void clearAllRateLimiters() {
        rateLimiters.clear();
        LOGGER.info("Cleared all RateLimiters");
    }
}