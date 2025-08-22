//package hoatv.kafka.notifier.service;
//
//import hoatv.kafka.notifier.model.NotificationAction;
//import hoatv.kafka.notifier.model.NotifierConfiguration;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Map;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("KafkaMessageProcessor Tests")
//class KafkaMessageProcessorTest {
//
//    @Mock
//    private NotifierConfigurationService configurationService;
//
//    @Mock
//    private RuleEvaluationService ruleEvaluationService;
//
//    @Mock
//    private NotificationService notificationService;
//
//    @InjectMocks
//    private KafkaMessageProcessor processor;
//
//    private NotifierConfiguration enabledConfiguration;
//    private NotifierConfiguration disabledConfiguration;
//    private NotificationAction slackAction;
//    private String testMessage;
//
//    @BeforeEach
//    void setUp() {
//        slackAction = NotificationAction.builder()
//                .type("call")
//                .params(Map.of(
//                    "provider", "SLACK",
//                    "webhookURL", "https://hooks.slack.com/services/xxx/yyy/zzz",
//                    "message", "CPU usage high: ${value}"
//                ))
//                .build();
//
//        enabledConfiguration = NotifierConfiguration.builder()
//                .id("config-1")
//                .notifier("CpuMonitor")
//                .topic("cpu-metrics")
//                .rules(Map.of("$gt", Map.of("$field", "value", "$value", 80)))
//                .actions(List.of(slackAction))
//                .enabled(true)
//                .description("CPU monitoring")
//                .createdAt(LocalDateTime.now())
//                .build();
//
//        disabledConfiguration = NotifierConfiguration.builder()
//                .id("config-2")
//                .notifier("MemoryMonitor")
//                .topic("memory-metrics")
//                .rules(Map.of("$gt", Map.of("$field", "value", "$value", 90)))
//                .actions(List.of(slackAction))
//                .enabled(false)
//                .description("Memory monitoring")
//                .createdAt(LocalDateTime.now())
//                .build();
//
//        testMessage = "{\"value\": 85, \"service\": \"api-server\", \"timestamp\": \"2024-01-15T10:30:00\"}";
//    }
//
//    @Test
//    @DisplayName("Should process message and trigger notification when rules match")
//    void shouldProcessMessageAndTriggerNotificationWhenRulesMatch() {
//        // Given
//        String topic = "cpu-metrics";
//        when(configurationService.findEnabledConfigurationsByTopic(topic))
//                .thenReturn(List.of(enabledConfiguration));
//        when(ruleEvaluationService.evaluateRules(
//                eq(enabledConfiguration.getRules()),
//                eq(testMessage)
//        )).thenReturn(true);
//
//        // When
//        processor.processMessage(testMessage, topic);
//
//        // Then
//        verify(configurationService).findEnabledConfigurationsByTopic(topic);
//        verify(ruleEvaluationService).evaluateRules(enabledConfiguration.getRules(), testMessage);
//        verify(notificationService).sendNotification(slackAction, testMessage);
//    }
//
//    @Test
//    @DisplayName("Should not trigger notification when rules don't match")
//    void shouldNotTriggerNotificationWhenRulesDoNotMatch() {
//        // Given
//        String topic = "cpu-metrics";
//        when(configurationService.findEnabledConfigurationsByTopic(topic))
//                .thenReturn(List.of(enabledConfiguration));
//        when(ruleEvaluationService.evaluateRules(
//                eq(enabledConfiguration.getRules()),
//                eq(testMessage)
//        )).thenReturn(false);
//
//        // When
//        processor.processMessage(testMessage, topic);
//
//        // Then
//        verify(configurationService).findEnabledConfigurationsByTopic(topic);
//        verify(ruleEvaluationService).evaluateRules(enabledConfiguration.getRules(), testMessage);
//        verify(notificationService, never()).sendNotification(any(), any());
//    }
//
//    @Test
//    @DisplayName("Should process multiple configurations for same topic")
//    void shouldProcessMultipleConfigurationsForSameTopic() {
//        // Given
//        String topic = "cpu-metrics";
//        NotifierConfiguration secondConfig = NotifierConfiguration.builder()
//                .id("config-3")
//                .notifier("SecondCpuMonitor")
//                .topic("cpu-metrics")
//                .rules(Map.of("$lt", Map.of("$field", "value", "$value", 90)))
//                .actions(List.of(slackAction))
//                .enabled(true)
//                .build();
//
//        when(configurationService.findEnabledConfigurationsByTopic(topic))
//                .thenReturn(List.of(enabledConfiguration, secondConfig));
//        when(ruleEvaluationService.evaluateRules(
//                eq(enabledConfiguration.getRules()),
//                eq(testMessage)
//        )).thenReturn(true);
//        when(ruleEvaluationService.evaluateRules(
//                eq(secondConfig.getRules()),
//                eq(testMessage)
//        )).thenReturn(true);
//
//        // When
//        processor.processMessage(testMessage, topic);
//
//        // Then
//        verify(configurationService).findEnabledConfigurationsByTopic(topic);
//        verify(ruleEvaluationService, times(2)).evaluateRules(any(), eq(testMessage));
//        verify(notificationService, times(2)).sendNotification(eq(slackAction), eq(testMessage));
//    }
//
//    @Test
//    @DisplayName("Should handle configuration with multiple actions")
//    void shouldHandleConfigurationWithMultipleActions() {
//        // Given
//        String topic = "cpu-metrics";
//        NotificationAction secondAction = NotificationAction.builder()
//                .type("call")
//                .params(Map.of(
//                    "provider", "SLACK",
//                    "webhookURL", "https://hooks.slack.com/services/aaa/bbb/ccc",
//                    "message", "Secondary alert: ${value}"
//                ))
//                .build();
//
//        enabledConfiguration.setActions(List.of(slackAction, secondAction));
//
//        when(configurationService.findEnabledConfigurationsByTopic(topic))
//                .thenReturn(List.of(enabledConfiguration));
//        when(ruleEvaluationService.evaluateRules(
//                eq(enabledConfiguration.getRules()),
//                eq(testMessage)
//        )).thenReturn(true);
//
//        // When
//        processor.processMessage(testMessage, topic);
//
//        // Then
//        verify(configurationService).findEnabledConfigurationsByTopic(topic);
//        verify(ruleEvaluationService).evaluateRules(enabledConfiguration.getRules(), testMessage);
//        verify(notificationService).sendNotification(slackAction, testMessage);
//        verify(notificationService).sendNotification(secondAction, testMessage);
//    }
//
//    @Test
//    @DisplayName("Should handle empty configurations list")
//    void shouldHandleEmptyConfigurationsList() {
//        // Given
//        String topic = "unknown-topic";
//        when(configurationService.findEnabledConfigurationsByTopic(topic))
//                .thenReturn(List.of());
//
//        // When
//        processor.processMessage(testMessage, topic);
//
//        // Then
//        verify(configurationService).findEnabledConfigurationsByTopic(topic);
//        verify(ruleEvaluationService, never()).evaluateRules(any(), any());
//        verify(notificationService, never()).sendNotification(any(), any());
//    }
//
//    @Test
//    @DisplayName("Should handle null configurations list")
//    void shouldHandleNullConfigurationsList() {
//        // Given
//        String topic = "unknown-topic";
//        when(configurationService.findEnabledConfigurationsByTopic(topic))
//                .thenReturn(null);
//
//        // When
//        processor.processMessage(testMessage, topic);
//
//        // Then
//        verify(configurationService).findEnabledConfigurationsByTopic(topic);
//        verify(ruleEvaluationService, never()).evaluateRules(any(), any());
//        verify(notificationService, never()).sendNotification(any(), any());
//    }
//
//    @Test
//    @DisplayName("Should handle configuration with no actions")
//    void shouldHandleConfigurationWithNoActions() {
//        // Given
//        String topic = "cpu-metrics";
//        enabledConfiguration.setActions(List.of());
//
//        when(configurationService.findEnabledConfigurationsByTopic(topic))
//                .thenReturn(List.of(enabledConfiguration));
//        when(ruleEvaluationService.evaluateRules(
//                eq(enabledConfiguration.getRules()),
//                eq(testMessage)
//        )).thenReturn(true);
//
//        // When
//        processor.processMessage(testMessage, topic);
//
//        // Then
//        verify(configurationService).findEnabledConfigurationsByTopic(topic);
//        verify(ruleEvaluationService).evaluateRules(enabledConfiguration.getRules(), testMessage);
//        verify(notificationService, never()).sendNotification(any(), any());
//    }
//
//    @Test
//    @DisplayName("Should handle configuration with null actions")
//    void shouldHandleConfigurationWithNullActions() {
//        // Given
//        String topic = "cpu-metrics";
//        enabledConfiguration.setActions(null);
//
//        when(configurationService.findEnabledConfigurationsByTopic(topic))
//                .thenReturn(List.of(enabledConfiguration));
//        when(ruleEvaluationService.evaluateRules(
//                eq(enabledConfiguration.getRules()),
//                eq(testMessage)
//        )).thenReturn(true);
//
//        // When
//        processor.processMessage(testMessage, topic);
//
//        // Then
//        verify(configurationService).findEnabledConfigurationsByTopic(topic);
//        verify(ruleEvaluationService).evaluateRules(enabledConfiguration.getRules(), testMessage);
//        verify(notificationService, never()).sendNotification(any(), any());
//    }
//
//    @Test
//    @DisplayName("Should handle exception in rule evaluation gracefully")
//    void shouldHandleExceptionInRuleEvaluationGracefully() {
//        // Given
//        String topic = "cpu-metrics";
//        when(configurationService.findEnabledConfigurationsByTopic(topic))
//                .thenReturn(List.of(enabledConfiguration));
//        when(ruleEvaluationService.evaluateRules(
//                eq(enabledConfiguration.getRules()),
//                eq(testMessage)
//        )).thenThrow(new RuntimeException("Rule evaluation failed"));
//
//        // When
//        processor.processMessage(testMessage, topic);
//
//        // Then
//        verify(configurationService).findEnabledConfigurationsByTopic(topic);
//        verify(ruleEvaluationService).evaluateRules(enabledConfiguration.getRules(), testMessage);
//        verify(notificationService, never()).sendNotification(any(), any());
//    }
//
//    @Test
//    @DisplayName("Should handle exception in notification service gracefully")
//    void shouldHandleExceptionInNotificationServiceGracefully() {
//        // Given
//        String topic = "cpu-metrics";
//        when(configurationService.findEnabledConfigurationsByTopic(topic))
//                .thenReturn(List.of(enabledConfiguration));
//        when(ruleEvaluationService.evaluateRules(
//                eq(enabledConfiguration.getRules()),
//                eq(testMessage)
//        )).thenReturn(true);
//        doThrow(new RuntimeException("Notification failed"))
//                .when(notificationService).sendNotification(any(), any());
//
//        // When
//        processor.processMessage(testMessage, topic);
//
//        // Then
//        verify(configurationService).findEnabledConfigurationsByTopic(topic);
//        verify(ruleEvaluationService).evaluateRules(enabledConfiguration.getRules(), testMessage);
//        verify(notificationService).sendNotification(slackAction, testMessage);
//    }
//
//    @Test
//    @DisplayName("Should continue processing other configurations when one fails")
//    void shouldContinueProcessingOtherConfigurationsWhenOneFails() {
//        // Given
//        String topic = "cpu-metrics";
//        NotifierConfiguration secondConfig = NotifierConfiguration.builder()
//                .id("config-3")
//                .notifier("SecondCpuMonitor")
//                .topic("cpu-metrics")
//                .rules(Map.of("$lt", Map.of("$field", "value", "$value", 90)))
//                .actions(List.of(slackAction))
//                .enabled(true)
//                .build();
//
//        when(configurationService.findEnabledConfigurationsByTopic(topic))
//                .thenReturn(List.of(enabledConfiguration, secondConfig));
//        when(ruleEvaluationService.evaluateRules(
//                eq(enabledConfiguration.getRules()),
//                eq(testMessage)
//        )).thenThrow(new RuntimeException("First config failed"));
//        when(ruleEvaluationService.evaluateRules(
//                eq(secondConfig.getRules()),
//                eq(testMessage)
//        )).thenReturn(true);
//
//        // When
//        processor.processMessage(testMessage, topic);
//
//        // Then
//        verify(configurationService).findEnabledConfigurationsByTopic(topic);
//        verify(ruleEvaluationService, times(2)).evaluateRules(any(), eq(testMessage));
//        verify(notificationService).sendNotification(slackAction, testMessage); // Only second config should trigger
//    }
//
//    @Test
//    @DisplayName("Should handle null message gracefully")
//    void shouldHandleNullMessageGracefully() {
//        // Given
//        String topic = "cpu-metrics";
//        String nullMessage = null;
//
//        // When
//        processor.processMessage(nullMessage, topic);
//
//        // Then
//        verify(configurationService, never()).findEnabledConfigurationsByTopic(any());
//        verify(ruleEvaluationService, never()).evaluateRules(any(), any());
//        verify(notificationService, never()).sendNotification(any(), any());
//    }
//
//    @Test
//    @DisplayName("Should handle null topic gracefully")
//    void shouldHandleNullTopicGracefully() {
//        // Given
//        String nullTopic = null;
//
//        // When
//        processor.processMessage(testMessage, nullTopic);
//
//        // Then
//        verify(configurationService, never()).findEnabledConfigurationsByTopic(any());
//        verify(ruleEvaluationService, never()).evaluateRules(any(), any());
//        verify(notificationService, never()).sendNotification(any(), any());
//    }
//}
