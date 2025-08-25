package com.hoatv.kafka.notifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoatv.kafka.notifier.client.SlackWebhookClient;
import com.hoatv.kafka.notifier.dto.SlackMessage;
import com.hoatv.kafka.notifier.model.NotificationAction;
import com.hoatv.kafka.notifier.model.NotifierConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test cases for NotificationService class
 * Tests focus on variable substitution in message templates
 */
@DisplayName("Notification Service Tests")
class NotificationServiceTest {

    @Mock
    private SlackWebhookClient slackWebhookClient;

    @InjectMocks
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<SlackMessage> slackMessageCaptor;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        MockitoAnnotations.openMocks(this);
        notificationService = new NotificationService(objectMapper, slackWebhookClient);
    }

    @Test
    @DisplayName("Should substitute ${value} placeholder with simple numeric value")
    void shouldSubstituteValuePlaceholderWithSimpleNumericValue() {
        // Given
        String message = "8";
        String messageTemplate = "CPU usage high: ${value}";
        String webhookUrl = "https://hooks.slack.com/services/test";

        Map<String, Object> params = new HashMap<>();
        params.put("provider", "SLACK");
        params.put("webhookURL", webhookUrl);
        params.put("message", messageTemplate);

        NotificationAction action = NotificationAction.builder()
                .type("call")
                .params(params)
                .build();

        NotifierConfiguration config = NotifierConfiguration.builder()
                .notifier("TestNotifier")
                .build();

        // When
        notificationService.executeNotificationAction(action, message, config);

        // Then
        verify(slackWebhookClient, times(1)).sendMessage(eq(webhookUrl), slackMessageCaptor.capture());
        SlackMessage capturedMessage = slackMessageCaptor.getValue();
        assertEquals("CPU usage high: 8", capturedMessage.getText());
    }

    @Test
    @DisplayName("Should substitute ${value} placeholder with simple string value")
    void shouldSubstituteValuePlaceholderWithSimpleStringValue() {
        // Given
        String message = "error";
        String messageTemplate = "System status: ${value}";
        String webhookUrl = "https://hooks.slack.com/services/test";

        Map<String, Object> params = new HashMap<>();
        params.put("provider", "SLACK");
        params.put("webhookURL", webhookUrl);
        params.put("message", messageTemplate);

        NotificationAction action = NotificationAction.builder()
                .type("call")
                .params(params)
                .build();

        NotifierConfiguration config = NotifierConfiguration.builder()
                .notifier("TestNotifier")
                .build();

        // When
        notificationService.executeNotificationAction(action, message, config);

        // Then
        verify(slackWebhookClient, times(1)).sendMessage(eq(webhookUrl), slackMessageCaptor.capture());
        SlackMessage capturedMessage = slackMessageCaptor.getValue();
        assertEquals("System status: error", capturedMessage.getText());
    }

    @Test
    @DisplayName("Should substitute field placeholders with JSON object values")
    void shouldSubstituteFieldPlaceholdersWithJsonObjectValues() {
        // Given
        String message = "{\"cpu\": 85, \"memory\": 70, \"status\": \"warning\"}";
        String messageTemplate = "Alert: CPU ${cpu}%, Memory ${memory}%, Status: ${status}";
        String webhookUrl = "https://hooks.slack.com/services/test";

        Map<String, Object> params = new HashMap<>();
        params.put("provider", "SLACK");
        params.put("webhookURL", webhookUrl);
        params.put("message", messageTemplate);

        NotificationAction action = NotificationAction.builder()
                .type("call")
                .params(params)
                .build();

        NotifierConfiguration config = NotifierConfiguration.builder()
                .notifier("TestNotifier")
                .build();

        // When
        notificationService.executeNotificationAction(action, message, config);

        // Then
        verify(slackWebhookClient, times(1)).sendMessage(eq(webhookUrl), slackMessageCaptor.capture());
        SlackMessage capturedMessage = slackMessageCaptor.getValue();
        assertEquals("Alert: CPU 85%, Memory 70%, Status: warning", capturedMessage.getText());
    }

    @Test
    @DisplayName("Should handle nested field placeholders")
    void shouldHandleNestedFieldPlaceholders() {
        // Given
        String message = "{\"system\": {\"cpu\": {\"usage\": 90}}}";
        String messageTemplate = "CPU usage: ${system.cpu.usage}%";
        String webhookUrl = "https://hooks.slack.com/services/test";

        Map<String, Object> params = new HashMap<>();
        params.put("provider", "SLACK");
        params.put("webhookURL", webhookUrl);
        params.put("message", messageTemplate);

        NotificationAction action = NotificationAction.builder()
                .type("call")
                .params(params)
                .build();

        NotifierConfiguration config = NotifierConfiguration.builder()
                .notifier("TestNotifier")
                .build();

        // When
        notificationService.executeNotificationAction(action, message, config);

        // Then
        verify(slackWebhookClient, times(1)).sendMessage(eq(webhookUrl), slackMessageCaptor.capture());
        SlackMessage capturedMessage = slackMessageCaptor.getValue();
        assertEquals("CPU usage: 90%", capturedMessage.getText());
    }

    @Test
    @DisplayName("Should preserve placeholder when field does not exist")
    void shouldPreservePlaceholderWhenFieldDoesNotExist() {
        // Given
        String message = "{\"cpu\": 85}";
        String messageTemplate = "CPU: ${cpu}%, Memory: ${memory}%";
        String webhookUrl = "https://hooks.slack.com/services/test";

        Map<String, Object> params = new HashMap<>();
        params.put("provider", "SLACK");
        params.put("webhookURL", webhookUrl);
        params.put("message", messageTemplate);

        NotificationAction action = NotificationAction.builder()
                .type("call")
                .params(params)
                .build();

        NotifierConfiguration config = NotifierConfiguration.builder()
                .notifier("TestNotifier")
                .build();

        // When
        notificationService.executeNotificationAction(action, message, config);

        // Then
        verify(slackWebhookClient, times(1)).sendMessage(eq(webhookUrl), slackMessageCaptor.capture());
        SlackMessage capturedMessage = slackMessageCaptor.getValue();
        assertEquals("CPU: 85%, Memory: ${memory}%", capturedMessage.getText());
    }

    @Test
    @DisplayName("Should handle decimal numbers correctly")
    void shouldHandleDecimalNumbersCorrectly() {
        // Given
        String message = "8.5";
        String messageTemplate = "Temperature: ${value}°C";
        String webhookUrl = "https://hooks.slack.com/services/test";

        Map<String, Object> params = new HashMap<>();
        params.put("provider", "SLACK");
        params.put("webhookURL", webhookUrl);
        params.put("message", messageTemplate);

        NotificationAction action = NotificationAction.builder()
                .type("call")
                .params(params)
                .build();

        NotifierConfiguration config = NotifierConfiguration.builder()
                .notifier("TestNotifier")
                .build();

        // When
        notificationService.executeNotificationAction(action, message, config);

        // Then
        verify(slackWebhookClient, times(1)).sendMessage(eq(webhookUrl), slackMessageCaptor.capture());
        SlackMessage capturedMessage = slackMessageCaptor.getValue();
        assertEquals("Temperature: 8.5°C", capturedMessage.getText());
    }
}
