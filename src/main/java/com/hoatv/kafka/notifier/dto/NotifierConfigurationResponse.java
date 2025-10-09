package com.hoatv.kafka.notifier.dto;

import com.hoatv.kafka.notifier.model.NotificationAction;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class NotifierConfigurationResponse {

    private String id;
    private String notifier;
    private String topic;
    private Map<String, Object> rules;
    private List<NotificationAction> actions;
    private boolean enabled;
    private String description;

    // Throttling configuration (optional - falls back to resilience4j.yml defaults)
    private Long throttlePeriodMinutes;

    private Integer throttlePermitsPerPeriod;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
