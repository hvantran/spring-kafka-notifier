package com.hoatv.kafka.notifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoatv.kafka.notifier.client.SlackWebhookClient;
import com.hoatv.kafka.notifier.dto.SlackMessage;
import com.hoatv.kafka.notifier.model.NotificationAction;
import com.hoatv.kafka.notifier.model.NotifierConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for NotificationService variable substitution
 * Tests the actual functionality without mocking
 */
@DisplayName("Notification Service Integration Tests")
class NotificationServiceIntegrationTest {

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        // Use a test implementation of SlackWebhookClient that doesn't actually send messages
        SlackWebhookClient testSlackClient = new TestSlackWebhookClient();
        notificationService = new NotificationService(objectMapper, testSlackClient);
    }

    @Test
    @DisplayName("Should substitute ${value} placeholder with simple numeric value from Kafka message")
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
        assertDoesNotThrow(() -> {
            notificationService.executeNotificationAction(action, message, config);
        });

        // Then - the test SlackWebhookClient will verify the message was processed correctly
        // We expect the ${value} placeholder to be replaced with "8"
    }

    @Test
    @DisplayName("Should substitute ${value} placeholder with decimal numeric value from Kafka message")
    void shouldSubstituteValuePlaceholderWithDecimalNumericValue() {
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

        // When & Then - should not throw any exceptions
        assertDoesNotThrow(() -> {
            notificationService.executeNotificationAction(action, message, config);
        });
    }

    /**
     * Test implementation of SlackWebhookClient that doesn't actually send HTTP requests
     */
    private static class TestSlackWebhookClient extends SlackWebhookClient {
        
        public TestSlackWebhookClient() {
            super(null); // We don't need a real RestTemplate for testing
        }
        
        @Override
        public void sendMessage(String webhookUrl, SlackMessage message) {
            // Verify that the message has the correct substitution
            String messageText = message.getText();
            
            // Assert that ${value} has been replaced and is not present in the final message
            assertFalse(messageText.contains("${value}"), 
                "Message should not contain unreplaced ${value} placeholder: " + messageText);
        }
    }
}
