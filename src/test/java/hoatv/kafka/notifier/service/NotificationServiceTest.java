package hoatv.kafka.notifier.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hoatv.kafka.notifier.client.SlackWebhookClient;
import hoatv.kafka.notifier.dto.SlackMessage;
import hoatv.kafka.notifier.model.NotificationAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Tests")
class NotificationServiceTest {

    @Mock
    private SlackWebhookClient slackWebhookClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationService service;

    private JsonNode mockJsonMessage;
    private NotificationAction slackAction;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        mockJsonMessage = mock(JsonNode.class);
        when(objectMapper.readTree(anyString())).thenReturn(mockJsonMessage);

        slackAction = NotificationAction.builder()
                .type("call")
                .params(Map.of(
                    "provider", "SLACK",
                    "webhookURL", "https://hooks.slack.com/services/xxx/yyy/zzz",
                    "message", "Alert: ${value} exceeded threshold"
                ))
                .build();
    }

    @Test
    @DisplayName("Should send Slack notification successfully")
    void shouldSendSlackNotificationSuccessfully() throws Exception {
        // Given
        String kafkaMessage = "{\"value\": 85, \"service\": \"api-server\"}";
        JsonNode valueNode = mock(JsonNode.class);
        when(valueNode.asText()).thenReturn("85");
        when(mockJsonMessage.path("value")).thenReturn(valueNode);

        // When
        service.sendNotification(slackAction, kafkaMessage);

        // Then
        verify(slackWebhookClient).sendMessage(
                eq("https://hooks.slack.com/services/xxx/yyy/zzz"),
                argThat(message -> message.getText().contains("Alert: 85 exceeded threshold"))
        );
        verify(objectMapper).readTree(kafkaMessage);
    }

    @Test
    @DisplayName("Should handle message template with multiple placeholders")
    void shouldHandleMessageTemplateWithMultiplePlaceholders() throws Exception {
        // Given
        String kafkaMessage = "{\"value\": 85, \"service\": \"api-server\", \"host\": \"server-01\"}";
        slackAction.getParams().put("message", "Service ${service} on ${host} has value ${value}");
        
        JsonNode valueNode = mock(JsonNode.class);
        when(valueNode.asText()).thenReturn("85");
        JsonNode serviceNode = mock(JsonNode.class);
        when(serviceNode.asText()).thenReturn("api-server");
        JsonNode hostNode = mock(JsonNode.class);
        when(hostNode.asText()).thenReturn("server-01");
        
        when(mockJsonMessage.path("value")).thenReturn(valueNode);
        when(mockJsonMessage.path("service")).thenReturn(serviceNode);
        when(mockJsonMessage.path("host")).thenReturn(hostNode);

        // When
        service.sendNotification(slackAction, kafkaMessage);

        // Then
        verify(slackWebhookClient).sendMessage(
                eq("https://hooks.slack.com/services/xxx/yyy/zzz"),
                argThat(message -> {
                    String text = message.getText();
                    return text.contains("Service api-server") && 
                           text.contains("on server-01") && 
                           text.contains("has value 85");
                })
        );
    }

    @Test
    @DisplayName("Should handle nested field placeholders")
    void shouldHandleNestedFieldPlaceholders() throws Exception {
        // Given
        String kafkaMessage = "{\"system\": {\"cpu\": {\"usage\": 75}}}";
        slackAction.getParams().put("message", "CPU usage is ${system.cpu.usage}%");
        
        JsonNode systemNode = mock(JsonNode.class);
        JsonNode cpuNode = mock(JsonNode.class);
        JsonNode usageNode = mock(JsonNode.class);
        when(usageNode.asText()).thenReturn("75");
        when(cpuNode.path("usage")).thenReturn(usageNode);
        when(systemNode.path("cpu")).thenReturn(cpuNode);
        when(mockJsonMessage.path("system")).thenReturn(systemNode);

        // When
        service.sendNotification(slackAction, kafkaMessage);

        // Then
        verify(slackWebhookClient).sendMessage(
                eq("https://hooks.slack.com/services/xxx/yyy/zzz"),
                argThat(message -> message.getText().contains("CPU usage is 75%"))
        );
    }

    @Test
    @DisplayName("Should handle missing field placeholders gracefully")
    void shouldHandleMissingFieldPlaceholdersGracefully() throws Exception {
        // Given
        String kafkaMessage = "{\"value\": 85}";
        slackAction.getParams().put("message", "Service ${service} has value ${value}");
        
        JsonNode valueNode = mock(JsonNode.class);
        when(valueNode.asText()).thenReturn("85");
        JsonNode missingNode = mock(JsonNode.class);
        when(missingNode.isMissingNode()).thenReturn(true);
        when(missingNode.asText()).thenReturn("");
        
        when(mockJsonMessage.path("value")).thenReturn(valueNode);
        when(mockJsonMessage.path("service")).thenReturn(missingNode);

        // When
        service.sendNotification(slackAction, kafkaMessage);

        // Then
        verify(slackWebhookClient).sendMessage(
                eq("https://hooks.slack.com/services/xxx/yyy/zzz"),
                argThat(message -> {
                    String text = message.getText();
                    return text.contains("Service  has value 85"); // Empty placeholder
                })
        );
    }

    @Test
    @DisplayName("Should use default message when template is empty")
    void shouldUseDefaultMessageWhenTemplateIsEmpty() throws Exception {
        // Given
        String kafkaMessage = "{\"value\": 85}";
        slackAction.getParams().put("message", "");

        // When
        service.sendNotification(slackAction, kafkaMessage);

        // Then
        verify(slackWebhookClient).sendMessage(
                eq("https://hooks.slack.com/services/xxx/yyy/zzz"),
                argThat(message -> message.getText().equals("Kafka message triggered notification"))
        );
    }

    @Test
    @DisplayName("Should use default message when template is null")
    void shouldUseDefaultMessageWhenTemplateIsNull() throws Exception {
        // Given
        String kafkaMessage = "{\"value\": 85}";
        slackAction.getParams().remove("message");

        // When
        service.sendNotification(slackAction, kafkaMessage);

        // Then
        verify(slackWebhookClient).sendMessage(
                eq("https://hooks.slack.com/services/xxx/yyy/zzz"),
                argThat(message -> message.getText().equals("Kafka message triggered notification"))
        );
    }

    @Test
    @DisplayName("Should handle invalid JSON gracefully")
    void shouldHandleInvalidJsonGracefully() throws Exception {
        // Given
        String invalidJson = "invalid-json";
        when(objectMapper.readTree(invalidJson)).thenThrow(new JsonProcessingException("Invalid JSON") {});

        // When
        service.sendNotification(slackAction, invalidJson);

        // Then
        verify(slackWebhookClient).sendMessage(
                eq("https://hooks.slack.com/services/xxx/yyy/zzz"),
                argThat(message -> message.getText().equals("Alert: ${value} exceeded threshold")) // Original template
        );
        verify(objectMapper).readTree(invalidJson);
    }

    @Test
    @DisplayName("Should not send notification for non-SLACK provider")
    void shouldNotSendNotificationForNonSlackProvider() throws Exception {
        // Given
        String kafkaMessage = "{\"value\": 85}";
        NotificationAction emailAction = NotificationAction.builder()
                .type("call")
                .params(Map.of(
                    "provider", "EMAIL",
                    "to", "admin@example.com"
                ))
                .build();

        // When
        service.sendNotification(emailAction, kafkaMessage);

        // Then
        verify(slackWebhookClient, never()).sendMessage(any(), any());
        verify(objectMapper, never()).readTree(any());
    }

    @Test
    @DisplayName("Should handle missing webhookURL gracefully")
    void shouldHandleMissingWebhookUrlGracefully() throws Exception {
        // Given
        String kafkaMessage = "{\"value\": 85}";
        slackAction.getParams().remove("webhookURL");

        // When & Then
        assertDoesNotThrow(() -> service.sendNotification(slackAction, kafkaMessage));
        verify(slackWebhookClient, never()).sendMessage(any(), any());
    }

    @Test
    @DisplayName("Should handle null webhookURL gracefully")
    void shouldHandleNullWebhookUrlGracefully() throws Exception {
        // Given
        String kafkaMessage = "{\"value\": 85}";
        slackAction.getParams().put("webhookURL", null);

        // When & Then
        assertDoesNotThrow(() -> service.sendNotification(slackAction, kafkaMessage));
        verify(slackWebhookClient, never()).sendMessage(any(), any());
    }

    @Test
    @DisplayName("Should handle empty webhookURL gracefully")
    void shouldHandleEmptyWebhookUrlGracefully() throws Exception {
        // Given
        String kafkaMessage = "{\"value\": 85}";
        slackAction.getParams().put("webhookURL", "");

        // When & Then
        assertDoesNotThrow(() -> service.sendNotification(slackAction, kafkaMessage));
        verify(slackWebhookClient, never()).sendMessage(any(), any());
    }

    @Test
    @DisplayName("Should create properly formatted SlackMessage")
    void shouldCreateProperlyFormattedSlackMessage() throws Exception {
        // Given
        String kafkaMessage = "{\"value\": 85, \"timestamp\": \"2024-01-15T10:30:00\"}";
        JsonNode valueNode = mock(JsonNode.class);
        when(valueNode.asText()).thenReturn("85");
        when(mockJsonMessage.path("value")).thenReturn(valueNode);

        // When
        service.sendNotification(slackAction, kafkaMessage);

        // Then
        verify(slackWebhookClient).sendMessage(
                eq("https://hooks.slack.com/services/xxx/yyy/zzz"),
                argThat(message -> {
                    assertTrue(message instanceof SlackMessage);
                    return message.getText().contains("Alert: 85 exceeded threshold");
                })
        );
    }

    @Test
    @DisplayName("Should handle complex placeholder patterns")
    void shouldHandleComplexPlaceholderPatterns() throws Exception {
        // Given
        String kafkaMessage = "{\"metrics\": {\"cpu\": 85, \"memory\": 75}}";
        slackAction.getParams().put("message", "CPU: ${metrics.cpu}%, Memory: ${metrics.memory}%, Sum: ${sum}");
        
        JsonNode metricsNode = mock(JsonNode.class);
        JsonNode cpuNode = mock(JsonNode.class);
        when(cpuNode.asText()).thenReturn("85");
        JsonNode memoryNode = mock(JsonNode.class);
        when(memoryNode.asText()).thenReturn("75");
        JsonNode missingNode = mock(JsonNode.class);
        when(missingNode.isMissingNode()).thenReturn(true);
        when(missingNode.asText()).thenReturn("");
        
        when(mockJsonMessage.path("metrics")).thenReturn(metricsNode);
        when(metricsNode.path("cpu")).thenReturn(cpuNode);
        when(metricsNode.path("memory")).thenReturn(memoryNode);
        when(mockJsonMessage.path("sum")).thenReturn(missingNode);

        // When
        service.sendNotification(slackAction, kafkaMessage);

        // Then
        verify(slackWebhookClient).sendMessage(
                eq("https://hooks.slack.com/services/xxx/yyy/zzz"),
                argThat(message -> {
                    String text = message.getText();
                    return text.contains("CPU: 85%") && 
                           text.contains("Memory: 75%") && 
                           text.contains("Sum: "); // Empty for missing field
                })
        );
    }
}
