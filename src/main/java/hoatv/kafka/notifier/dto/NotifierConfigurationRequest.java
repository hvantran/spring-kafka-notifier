package hoatv.kafka.notifier.dto;

import hoatv.kafka.notifier.model.NotificationAction;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class NotifierConfigurationRequest {
    
    @NotBlank(message = "Notifier name is required")
    private String notifier;
    
    @NotBlank(message = "Topic is required")
    private String topic;
    
    @NotNull(message = "Rules are required")
    private Map<String, Object> rules;
    
    @NotNull(message = "Actions are required")
    @Valid
    private List<NotificationAction> actions;
    
    @Builder.Default
    private Boolean enabled = true;
    
    private String description;
}
