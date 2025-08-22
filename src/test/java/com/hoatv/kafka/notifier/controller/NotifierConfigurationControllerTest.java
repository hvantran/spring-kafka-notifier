/*
package com.hoatv.kafka.notifier.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoatv.kafka.notifier.dto.NotifierConfigurationRequest;
import com.hoatv.kafka.notifier.dto.NotifierConfigurationResponse;
import com.hoatv.kafka.notifier.exception.DuplicateResourceException;
import com.hoatv.kafka.notifier.exception.ResourceNotFoundException;
import com.hoatv.kafka.notifier.model.NotificationAction;
import com.hoatv.kafka.notifier.service.NotifierConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotifierConfigurationController.class)
@DisplayName("NotifierConfigurationController Tests")
class NotifierConfigurationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotifierConfigurationService service;

    @Autowired
    private ObjectMapper objectMapper;

    private NotifierConfigurationRequest validRequest;
    private NotifierConfigurationResponse validResponse;
    private NotificationAction slackAction;

    @BeforeEach
    void setUp() {
        slackAction = NotificationAction.builder()
                .type("call")
                .params(Map.of(
                    "provider", "SLACK",
                    "webhookURL", "https://hooks.slack.com/services/xxx/yyy/zzz",
                    "message", "CPU usage high: ${value}"
                ))
                .build();

        validRequest = NotifierConfigurationRequest.builder()
                .notifier("MetricThresholdNotifier")
                .topic("cpu-usage")
                .rules(Map.of("$gt", Map.of("$field", "value", "$value", 80)))
                .actions(List.of(slackAction))
                .enabled(true)
                .description("CPU usage monitoring")
                .build();

        validResponse = NotifierConfigurationResponse.builder()
                .id("config-id-123")
                .notifier("MetricThresholdNotifier")
                .topic("cpu-usage")
                .rules(Map.of("$gt", Map.of("$field", "value", "$value", 80)))
                .actions(List.of(slackAction))
                .enabled(true)
                .description("CPU usage monitoring")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/configurations - Should create configuration successfully")
    void shouldCreateConfigurationSuccessfully() throws Exception {
        // Given
        when(service.create(any(NotifierConfigurationRequest.class)))
                .thenReturn(validResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/configurations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("config-id-123"))
                .andExpect(jsonPath("$.notifier").value("MetricThresholdNotifier"))
                .andExpect(jsonPath("$.topic").value("cpu-usage"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.description").value("CPU usage monitoring"));

        verify(service).create(any(NotifierConfigurationRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/configurations - Should return 409 for duplicate configuration")
    void shouldReturn409ForDuplicateConfiguration() throws Exception {
        // Given
        when(service.create(any(NotifierConfigurationRequest.class)))
                .thenThrow(new DuplicateResourceException("Configuration already exists"));

        // When & Then
        mockMvc.perform(post("/api/v1/configurations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Configuration already exists"));

        verify(service).create(any(NotifierConfigurationRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/configurations - Should return 400 for invalid request")
    void shouldReturn400ForInvalidRequest() throws Exception {
        // Given
        NotifierConfigurationRequest invalidRequest = NotifierConfigurationRequest.builder()
                .notifier("") // Invalid: empty notifier
                .topic("cpu-usage")
                .rules(Map.of())
                .actions(List.of())
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/configurations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("PUT /api/v1/configurations/{id} - Should update configuration successfully")
    void shouldUpdateConfigurationSuccessfully() throws Exception {
        // Given
        String configId = "config-id-123";
        when(service.update(eq(configId), any(NotifierConfigurationRequest.class)))
                .thenReturn(validResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/configurations/{id}", configId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("config-id-123"))
                .andExpect(jsonPath("$.notifier").value("MetricThresholdNotifier"));

        verify(service).update(eq(configId), any(NotifierConfigurationRequest.class));
    }

    @Test
    @DisplayName("PUT /api/v1/configurations/{id} - Should return 404 for non-existent configuration")
    void shouldReturn404ForNonExistentConfigurationOnUpdate() throws Exception {
        // Given
        String configId = "non-existent-id";
        when(service.update(eq(configId), any(NotifierConfigurationRequest.class)))
                .thenThrow(new ResourceNotFoundException("NotifierConfiguration not found"));

        // When & Then
        mockMvc.perform(put("/api/v1/configurations/{id}", configId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("NotifierConfiguration not found"));

        verify(service).update(eq(configId), any(NotifierConfigurationRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/configurations/{id} - Should get configuration by ID successfully")
    void shouldGetConfigurationByIdSuccessfully() throws Exception {
        // Given
        String configId = "config-id-123";
        when(service.findById(configId)).thenReturn(validResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/configurations/{id}", configId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("config-id-123"))
                .andExpect(jsonPath("$.notifier").value("MetricThresholdNotifier"))
                .andExpect(jsonPath("$.topic").value("cpu-usage"));

        verify(service).findById(configId);
    }

    @Test
    @DisplayName("GET /api/v1/configurations/{id} - Should return 404 for non-existent configuration")
    void shouldReturn404ForNonExistentConfiguration() throws Exception {
        // Given
        String configId = "non-existent-id";
        when(service.findById(configId))
                .thenThrow(new ResourceNotFoundException("NotifierConfiguration not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/configurations/{id}", configId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("NotifierConfiguration not found"));

        verify(service).findById(configId);
    }

    @Test
    @DisplayName("GET /api/v1/configurations - Should get all configurations with pagination")
    void shouldGetAllConfigurationsWithPagination() throws Exception {
        // Given
        Page<NotifierConfigurationResponse> mockPage = new PageImpl<>(
                List.of(validResponse),
                PageRequest.of(0, 10),
                1
        );
        when(service.findAll(any())).thenReturn(mockPage);

        // When & Then
        mockMvc.perform(get("/api/v1/configurations")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value("config-id-123"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));

        verify(service).findAll(any());
    }

    @Test
    @DisplayName("GET /api/v1/configurations - Should use default pagination parameters")
    void shouldUseDefaultPaginationParameters() throws Exception {
        // Given
        Page<NotifierConfigurationResponse> mockPage = new PageImpl<>(
                List.of(validResponse),
                PageRequest.of(0, 20),
                1
        );
        when(service.findAll(any())).thenReturn(mockPage);

        // When & Then
        mockMvc.perform(get("/api/v1/configurations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0));

        verify(service).findAll(argThat(pageable -> 
            pageable.getPageNumber() == 0 && pageable.getPageSize() == 20));
    }

    @Test
    @DisplayName("GET /api/v1/configurations/topic/{topic} - Should get configurations by topic")
    void shouldGetConfigurationsByTopic() throws Exception {
        // Given
        String topic = "cpu-usage";
        when(service.findByTopic(topic)).thenReturn(List.of(validResponse));

        // When & Then
        mockMvc.perform(get("/api/v1/configurations/topic/{topic}", topic))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("config-id-123"))
                .andExpect(jsonPath("$[0].topic").value("cpu-usage"));

        verify(service).findByTopic(topic);
    }

    @Test
    @DisplayName("GET /api/v1/configurations/enabled - Should get enabled configurations")
    void shouldGetEnabledConfigurations() throws Exception {
        // Given
        when(service.findEnabledConfigurations()).thenReturn(List.of(validResponse));

        // When & Then
        mockMvc.perform(get("/api/v1/configurations/enabled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("config-id-123"))
                .andExpect(jsonPath("$[0].enabled").value(true));

        verify(service).findEnabledConfigurations();
    }

    @Test
    @DisplayName("DELETE /api/v1/configurations/{id} - Should delete configuration successfully")
    void shouldDeleteConfigurationSuccessfully() throws Exception {
        // Given
        String configId = "config-id-123";
        doNothing().when(service).delete(configId);

        // When & Then
        mockMvc.perform(delete("/api/v1/configurations/{id}", configId))
                .andExpect(status().isNoContent());

        verify(service).delete(configId);
    }

    @Test
    @DisplayName("DELETE /api/v1/configurations/{id} - Should return 404 for non-existent configuration")
    void shouldReturn404ForNonExistentConfigurationOnDelete() throws Exception {
        // Given
        String configId = "non-existent-id";
        doThrow(new ResourceNotFoundException("NotifierConfiguration not found"))
                .when(service).delete(configId);

        // When & Then
        mockMvc.perform(delete("/api/v1/configurations/{id}", configId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("NotifierConfiguration not found"));

        verify(service).delete(configId);
    }

    @Test
    @DisplayName("PATCH /api/v1/configurations/{id}/toggle - Should toggle enabled status successfully")
    void shouldToggleEnabledStatusSuccessfully() throws Exception {
        // Given
        String configId = "config-id-123";
        NotifierConfigurationResponse toggledResponse = validResponse.toBuilder()
                .enabled(false)
                .build();
        when(service.toggleEnabled(configId)).thenReturn(toggledResponse);

        // When & Then
        mockMvc.perform(patch("/api/v1/configurations/{id}/toggle", configId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("config-id-123"))
                .andExpect(jsonPath("$.enabled").value(false));

        verify(service).toggleEnabled(configId);
    }

    @Test
    @DisplayName("PATCH /api/v1/configurations/{id}/toggle - Should return 404 for non-existent configuration")
    void shouldReturn404ForNonExistentConfigurationOnToggle() throws Exception {
        // Given
        String configId = "non-existent-id";
        when(service.toggleEnabled(configId))
                .thenThrow(new ResourceNotFoundException("NotifierConfiguration not found"));

        // When & Then
        mockMvc.perform(patch("/api/v1/configurations/{id}/toggle", configId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("NotifierConfiguration not found"));

        verify(service).toggleEnabled(configId);
    }

    @Test
    @DisplayName("Should handle malformed JSON request")
    void shouldHandleMalformedJsonRequest() throws Exception {
        // Given
        String malformedJson = "{\"notifier\": \"Test\", \"topic\": }"; // Invalid JSON

        // When & Then
        mockMvc.perform(post("/api/v1/configurations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("Should handle missing Content-Type header")
    void shouldHandleMissingContentTypeHeader() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/configurations")
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnsupportedMediaType());

        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("Should validate required fields")
    void shouldValidateRequiredFields() throws Exception {
        // Given
        NotifierConfigurationRequest invalidRequest = NotifierConfigurationRequest.builder()
                // Missing notifier and topic
                .rules(Map.of())
                .actions(List.of())
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/configurations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any());
    }
}
*/
