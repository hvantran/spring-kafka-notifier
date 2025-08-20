package hoatv.kafka.notifier.dto;

import hoatv.kafka.notifier.model.NotificationAction;
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
@Builder
public class NotifierConfigurationResponse {
    
    private String id;
    private String notifier;
    private String topic;
    private Map<String, Object> rules;
    private List<NotificationAction> actions;
    private boolean enabled;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
