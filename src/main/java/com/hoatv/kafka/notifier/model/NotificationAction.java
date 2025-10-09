package com.hoatv.kafka.notifier.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationAction {

    @NotBlank(message = "Action type is required")
    private String type;

    @NotNull(message = "Action parameters are required")
    private Map<String, Object> params;
}
