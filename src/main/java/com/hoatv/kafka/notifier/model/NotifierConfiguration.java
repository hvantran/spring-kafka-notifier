package com.hoatv.kafka.notifier.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "notifier_configurations")
public class NotifierConfiguration {

    @Id
    @Builder.Default
    private String id =  UUID.randomUUID().toString();
    
    @NotBlank(message = "Notifier name is required")
    private String notifier;
    
    @NotBlank(message = "Topic is required")
    private String topic;
    
    @NotNull(message = "Rules are required")
    private Map<String, Object> rules;
    
    @NotNull(message = "Actions are required")
    private List<NotificationAction> actions;
    
    @Builder.Default
    private boolean enabled = true;
    
    private String description;
    
    // Throttling configuration (optional - falls back to resilience4j.yml defaults)
    private Long throttlePeriodMinutes; // null = use default from resilience4j.yml
    
    private Integer throttlePermitsPerPeriod; // null = use default from resilience4j.yml
    
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt;
    
    private String createdBy;
    
    private String updatedBy;
}
