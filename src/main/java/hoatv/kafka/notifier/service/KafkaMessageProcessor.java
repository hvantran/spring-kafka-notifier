package hoatv.kafka.notifier.service;

import hoatv.kafka.notifier.model.NotifierConfiguration;
import hoatv.kafka.notifier.model.NotificationAction;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KafkaMessageProcessor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMessageProcessor.class);
    
    private final NotifierConfigurationService configurationService;
    private final RuleEvaluationService ruleEvaluationService;
    private final NotificationService notificationService;
    
    @KafkaListener(topics = "#{kafkaTopicService.getAllTopics()}", 
                   groupId = "${spring.kafka.consumer.group-id}")
    public void processMessage(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        LOGGER.debug("Received message from topic '{}': {}", topic, message);
        
        try {
            // Find all enabled configurations for this topic
            List<NotifierConfiguration> configurations = 
                configurationService.findEnabledConfigurationsByTopic(topic);
            
            if (configurations.isEmpty()) {
                LOGGER.debug("No enabled configurations found for topic: {}", topic);
                return;
            }
            
            LOGGER.info("Processing {} configurations for topic: {}", configurations.size(), topic);
            
            // Process each configuration
            for (NotifierConfiguration config : configurations) {
                processConfigurationForMessage(config, message, topic);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error processing message from topic '{}': {}", topic, e.getMessage(), e);
        }
    }
    
    private void processConfigurationForMessage(NotifierConfiguration config, 
                                              String message, String topic) {
        try {
            LOGGER.debug("Evaluating rules for configuration: {} on topic: {}", 
                    config.getNotifier(), topic);
            
            // Evaluate rules against the message
            boolean rulesMatch = ruleEvaluationService.evaluateRules(
                config.getRules(), message);
            
            if (rulesMatch) {
                LOGGER.info("Rules matched for configuration: {} on topic: {}. Executing actions.", 
                        config.getNotifier(), topic);
                
                // Execute all actions for this configuration
                for (NotificationAction action : config.getActions()) {
                    executeAction(action, message, config);
                }
            } else {
                LOGGER.debug("Rules did not match for configuration: {} on topic: {}", 
                        config.getNotifier(), topic);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error processing configuration '{}' for topic '{}': {}", 
                    config.getNotifier(), topic, e.getMessage(), e);
        }
    }
    
    private void executeAction(NotificationAction action, String message, 
                             NotifierConfiguration config) {
        try {
            LOGGER.debug("Executing action of type: {} for configuration: {}", 
                    action.getType(), config.getNotifier());
            
            switch (action.getType().toLowerCase()) {
                case "call":
                    notificationService.executeNotificationAction(action, message, config);
                    break;
                default:
                    LOGGER.warn("Unsupported action type: {} for configuration: {}", 
                            action.getType(), config.getNotifier());
            }
            
        } catch (Exception e) {
            LOGGER.error("Error executing action of type '{}' for configuration '{}': {}", 
                    action.getType(), config.getNotifier(), e.getMessage(), e);
        }
    }
}
