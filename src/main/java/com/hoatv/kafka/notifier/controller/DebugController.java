package com.hoatv.kafka.notifier.controller;

import com.hoatv.kafka.notifier.service.NotificationThrottlingService;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Debug controller for testing rate limiting configuration.
 * This helps verify that the resilience4j.yml configuration is being loaded correctly.
 */
@RestController
@RequestMapping("/api/debug")
@Slf4j
public class DebugController {

    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;
    
    @Autowired
    private NotificationThrottlingService throttlingService;

    /**
     * Get the default rate limiter configuration.
     */
    @GetMapping("/rate-limiter-config")
    public Map<String, Object> getRateLimiterConfig() {
        RateLimiterConfig defaultConfig = rateLimiterRegistry.getDefaultConfig();
        
        Map<String, Object> configInfo = new HashMap<>();
        configInfo.put("limitForPeriod", defaultConfig.getLimitForPeriod());
        configInfo.put("limitRefreshPeriod", defaultConfig.getLimitRefreshPeriod().toString());
        configInfo.put("timeoutDuration", defaultConfig.getTimeoutDuration().toString());
        configInfo.put("message", "Configuration loaded from resilience4j.yml");
        
        log.info("Rate limiter configuration requested: {}", configInfo);
        return configInfo;
    }

    /**
     * Test the throttling service with a sample notifier.
     */
    @GetMapping("/test-throttling")
    public Map<String, Object> testThrottling(@RequestParam(defaultValue = "debug-test") String notifierName) {
        boolean allowed = throttlingService.shouldSendNotification(notifierName);
        
        Map<String, Object> result = new HashMap<>();
        result.put("notifierName", notifierName);
        result.put("allowed", allowed);
        result.put("timestamp", System.currentTimeMillis());
        result.put("message", allowed ? "Notification allowed" : "Notification throttled");
        
        log.info("Throttling test for notifier '{}': {}", notifierName, allowed ? "ALLOWED" : "THROTTLED");
        return result;
    }

    /**
     * Clear rate limiter for a specific notifier (useful for testing).
     */
    @GetMapping("/clear-rate-limiter")
    public Map<String, Object> clearRateLimiter(@RequestParam String notifierName) {
        throttlingService.clearRateLimiter(notifierName);
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Rate limiter cleared for notifier: " + notifierName);
        result.put("notifierName", notifierName);
        
        log.info("Rate limiter cleared for notifier: {}", notifierName);
        return result;
    }
}
