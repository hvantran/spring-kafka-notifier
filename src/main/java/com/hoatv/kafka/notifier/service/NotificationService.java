package com.hoatv.kafka.notifier.service;

import com.hoatv.kafka.notifier.client.SlackWebhookClient;
import com.hoatv.kafka.notifier.dto.SlackMessage;
import com.hoatv.kafka.notifier.model.NotifierConfiguration;
import com.hoatv.kafka.notifier.model.NotificationAction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    private final ObjectMapper objectMapper;
    private final SlackWebhookClient slackWebhookClient;
    
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    
    public void executeNotificationAction(NotificationAction action, String message, NotifierConfiguration config) {
        try {
            String provider = (String) action.getParams().get("provider");
            if (provider.equals("SLACK")) {
                sendSlackNotification(action, message, config);
                return;
            }
            LOGGER.warn("Unsupported notification provider: {}", provider);

        } catch (Exception e) {
            LOGGER.error("Error executing notification action: {}", e.getMessage(), e);
        }
    }
    
    private void sendSlackNotification(NotificationAction action, String message, NotifierConfiguration config) {
        try {
            String webhookUrl = (String) action.getParams().get("webhookURL");
            String messageTemplate = (String) action.getParams().get("message");
            
            if (webhookUrl == null || messageTemplate == null) {
                LOGGER.error("Missing required parameters for Slack notification. webhookURL: {}, message: {}", 
                        webhookUrl != null, messageTemplate != null);
                return;
            }
            
            JsonNode messageNode = objectMapper.readTree(message);
            String finalMessage = replaceVariables(messageTemplate, messageNode);
            
            SlackMessage slackMessage = SlackMessage.of(finalMessage);
            slackWebhookClient.sendMessage(webhookUrl, slackMessage);
            LOGGER.info("Successfully sent Slack notification for notifier: {}", config.getNotifier());
        } catch (Exception e) {
            LOGGER.error("Error sending Slack notification for configuration '{}': {}", config.getNotifier(), e.getMessage(), e);
        }
    }
    
    private String replaceVariables(String template, JsonNode messageNode) {
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String replacement = getVariableValue(variableName, messageNode);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
    
    private String getVariableValue(String variableName, JsonNode messageNode) {
        try {
            if (variableName.contains(".")) {
                String[] parts = variableName.split("\\.");
                JsonNode current = messageNode;
                
                for (String part : parts) {
                    if (current == null) {
                        break;
                    }
                    current = current.get(part);
                }
                
                return current != null ? current.asText() : "${" + variableName + "}";
            } else {
                JsonNode fieldNode = messageNode.get(variableName);
                return fieldNode != null ? fieldNode.asText() : "${" + variableName + "}";
            }
        } catch (Exception e) {
            LOGGER.warn("Error extracting variable '{}' from message: {}", variableName, e.getMessage());
            return "${" + variableName + "}";
        }
    }
    
    // Method expected by tests - delegates to executeNotificationAction
    public void sendNotification(NotificationAction action, String message) {
        executeNotificationAction(action, message, null);
    }
}
