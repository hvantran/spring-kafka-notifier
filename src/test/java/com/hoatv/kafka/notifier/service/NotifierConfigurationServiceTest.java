package com.hoatv.kafka.notifier.service;

import com.hoatv.kafka.notifier.dto.NotifierConfigurationRequest;
import com.hoatv.kafka.notifier.dto.NotifierConfigurationResponse;
import com.hoatv.kafka.notifier.exception.DuplicateResourceException;
import com.hoatv.kafka.notifier.exception.ResourceNotFoundException;
import com.hoatv.kafka.notifier.model.NotificationAction;
import com.hoatv.kafka.notifier.model.NotifierConfiguration;
import com.hoatv.kafka.notifier.repository.NotifierConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotifierConfigurationService Tests")
class NotifierConfigurationServiceTest {

    @Mock
    private NotifierConfigurationRepository repository;

    @InjectMocks
    private NotifierConfigurationService service;

    private NotifierConfigurationRequest validRequest;
    private NotifierConfiguration savedConfiguration;
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

        savedConfiguration = NotifierConfiguration.builder()
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
    @DisplayName("Should create configuration successfully")
    void shouldCreateConfigurationSuccessfully() {
        // Given
        when(repository.existsByNotifierAndTopic("MetricThresholdNotifier", "cpu-usage"))
                .thenReturn(false);
        when(repository.save(any(NotifierConfiguration.class)))
                .thenReturn(savedConfiguration);

        // When
        NotifierConfigurationResponse response = service.create(validRequest);

        // Then
        assertNotNull(response);
        assertEquals("config-id-123", response.getId());
        assertEquals("MetricThresholdNotifier", response.getNotifier());
        assertEquals("cpu-usage", response.getTopic());
        assertTrue(response.isEnabled());
        assertEquals("CPU usage monitoring", response.getDescription());

        verify(repository).existsByNotifierAndTopic("MetricThresholdNotifier", "cpu-usage");
        verify(repository).save(any(NotifierConfiguration.class));
    }

    @Test
    @DisplayName("Should throw exception when creating duplicate configuration")
    void shouldThrowExceptionWhenCreatingDuplicateConfiguration() {
        // Given
        when(repository.existsByNotifierAndTopic("MetricThresholdNotifier", "cpu-usage"))
                .thenReturn(true);

        // When & Then
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> service.create(validRequest)
        );

        assertTrue(exception.getMessage().contains("Configuration already exists"));
        verify(repository).existsByNotifierAndTopic("MetricThresholdNotifier", "cpu-usage");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should update configuration successfully")
    void shouldUpdateConfigurationSuccessfully() {
        // Given
        String configId = "config-id-123";
        when(repository.findById(configId)).thenReturn(Optional.of(savedConfiguration));
        when(repository.save(any(NotifierConfiguration.class))).thenReturn(savedConfiguration);

        // When
        NotifierConfigurationResponse response = service.update(configId, validRequest);

        // Then
        assertNotNull(response);
        assertEquals(configId, response.getId());
        assertEquals("MetricThresholdNotifier", response.getNotifier());

        verify(repository).findById(configId);
        verify(repository).save(any(NotifierConfiguration.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent configuration")
    void shouldThrowExceptionWhenUpdatingNonExistentConfiguration() {
        // Given
        String configId = "non-existent-id";
        when(repository.findById(configId)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.update(configId, validRequest)
        );

        assertTrue(exception.getMessage().contains("NotifierConfiguration not found"));
        verify(repository).findById(configId);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should find configuration by ID successfully")
    void shouldFindConfigurationByIdSuccessfully() {
        // Given
        String configId = "config-id-123";
        when(repository.findById(configId)).thenReturn(Optional.of(savedConfiguration));

        // When
        NotifierConfigurationResponse response = service.findById(configId);

        // Then
        assertNotNull(response);
        assertEquals(configId, response.getId());
        assertEquals("MetricThresholdNotifier", response.getNotifier());

        verify(repository).findById(configId);
    }

    @Test
    @DisplayName("Should throw exception when finding non-existent configuration")
    void shouldThrowExceptionWhenFindingNonExistentConfiguration() {
        // Given
        String configId = "non-existent-id";
        when(repository.findById(configId)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.findById(configId)
        );

        assertTrue(exception.getMessage().contains("NotifierConfiguration not found"));
        verify(repository).findById(configId);
    }

    @Test
    @DisplayName("Should find all configurations with pagination")
    void shouldFindAllConfigurationsWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<NotifierConfiguration> mockPage = new PageImpl<>(List.of(savedConfiguration));
        when(repository.findAll(pageable)).thenReturn(mockPage);

        // When
        Page<NotifierConfigurationResponse> response = service.findAll(pageable);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("config-id-123", response.getContent().get(0).getId());

        verify(repository).findAll(pageable);
    }

    @Test
    @DisplayName("Should find configurations by topic")
    void shouldFindConfigurationsByTopic() {
        // Given
        String topic = "cpu-usage";
        when(repository.findByTopic(topic)).thenReturn(List.of(savedConfiguration));

        // When
        List<NotifierConfigurationResponse> response = service.findByTopic(topic);

        // Then
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("config-id-123", response.get(0).getId());
        assertEquals(topic, response.get(0).getTopic());

        verify(repository).findByTopic(topic);
    }

    @Test
    @DisplayName("Should find enabled configurations")
    void shouldFindEnabledConfigurations() {
        // Given
        when(repository.findByEnabledTrue()).thenReturn(List.of(savedConfiguration));

        // When
        List<NotifierConfigurationResponse> response = service.findEnabledConfigurations();

        // Then
        assertNotNull(response);
        assertEquals(1, response.size());
        assertTrue(response.get(0).isEnabled());

        verify(repository).findByEnabledTrue();
    }

    @Test
    @DisplayName("Should find enabled configurations by topic")
    void shouldFindEnabledConfigurationsByTopic() {
        // Given
        String topic = "cpu-usage";
        when(repository.findByTopicAndEnabledTrue(topic)).thenReturn(List.of(savedConfiguration));

        // When
        List<NotifierConfiguration> response = service.findEnabledConfigurationsByTopic(topic);

        // Then
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("config-id-123", response.get(0).getId());
        assertTrue(response.get(0).isEnabled());

        verify(repository).findByTopicAndEnabledTrue(topic);
    }

    @Test
    @DisplayName("Should delete configuration successfully")
    void shouldDeleteConfigurationSuccessfully() {
        // Given
        String configId = "config-id-123";
        when(repository.existsById(configId)).thenReturn(true);

        // When
        service.delete(configId);

        // Then
        verify(repository).existsById(configId);
        verify(repository).deleteById(configId);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent configuration")
    void shouldThrowExceptionWhenDeletingNonExistentConfiguration() {
        // Given
        String configId = "non-existent-id";
        when(repository.existsById(configId)).thenReturn(false);

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.delete(configId)
        );

        assertTrue(exception.getMessage().contains("NotifierConfiguration not found"));
        verify(repository).existsById(configId);
        verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should toggle enabled status successfully")
    void shouldToggleEnabledStatusSuccessfully() {
        // Given
        String configId = "config-id-123";
        savedConfiguration.setEnabled(true);
        when(repository.findById(configId)).thenReturn(Optional.of(savedConfiguration));
        when(repository.save(any(NotifierConfiguration.class))).thenReturn(savedConfiguration);

        // When
        NotifierConfigurationResponse response = service.toggleEnabled(configId);

        // Then
        assertNotNull(response);
        verify(repository).findById(configId);
        verify(repository).save(argThat(config -> !config.isEnabled())); // Should be toggled to false
    }

    @Test
    @DisplayName("Should throw exception when toggling non-existent configuration")
    void shouldThrowExceptionWhenTogglingNonExistentConfiguration() {
        // Given
        String configId = "non-existent-id";
        when(repository.findById(configId)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.toggleEnabled(configId)
        );

        assertTrue(exception.getMessage().contains("NotifierConfiguration not found"));
        verify(repository).findById(configId);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle enabled field as null in request")
    void shouldHandleEnabledFieldAsNullInRequest() {
        // Given
        validRequest.setEnabled(null);
        when(repository.existsByNotifierAndTopic(any(), any())).thenReturn(false);
        when(repository.save(any(NotifierConfiguration.class))).thenReturn(savedConfiguration);

        // When
        NotifierConfigurationResponse response = service.create(validRequest);

        // Then
        assertNotNull(response);
        verify(repository).save(argThat(config -> config.isEnabled())); // Should default to true
    }
}
