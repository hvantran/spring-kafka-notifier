package com.hoatv.kafka.notifier.service;

import com.hoatv.kafka.notifier.client.SlackWebhookClient;
import com.hoatv.kafka.notifier.dto.SlackMessage;
import com.hoatv.kafka.notifier.model.NotifierConfiguration;
import com.hoatv.kafka.notifier.model.NotificationAction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    private final ObjectMapper objectMapper;
    private final SlackWebhookClient slackWebhookClient;

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

            JsonNode messageNode = getMessageNode(message);
            String finalMessage = replaceVariables(messageTemplate, messageNode);
            SlackMessage slackMessage = SlackMessage.of(finalMessage);
            slackWebhookClient.sendMessage(webhookUrl, slackMessage);
            LOGGER.info("Successfully sent Slack notification for notifier: {}", config.getNotifier());
        } catch (Exception e) {
            LOGGER.error("Error sending Slack notification for configuration '{}': {}", config.getNotifier(), e.getMessage(), e);
        }
    }

    private JsonNode getMessageNode(String message) {
        JsonNode messageNode;
        try {
            messageNode = objectMapper.readTree(message);
        } catch (Exception e) {
            messageNode = objectMapper.valueToTree(message);
        }
        return messageNode;
    }

    private String replaceVariables(String template, JsonNode messageNode) {
        Map<String, String> variableMap = createVariableMap(messageNode);
        StringSubstitutor substitutor = new StringSubstitutor(variableMap);
        return substitutor.replace(template);
    }

    /**
     * Create a map of variable names to their values from the JSON message
     */
    private Map<String, String> createVariableMap(JsonNode messageNode) {
        Map<String, String> variableMap = new HashMap<>();

        if (!messageNode.isObject() && !messageNode.isArray()) {
            variableMap.put("value", messageNode.asText());
            return variableMap;
        }

        if (messageNode.isObject()) {
            flattenJsonToMap(messageNode, "", variableMap);
        }

        return variableMap;
    }

    /**
     * Recursively flatten JSON object into a map with dot-notation keys
     */
    private void flattenJsonToMap(JsonNode node, String prefix, Map<String, String> map) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = prefix.isEmpty() ? field.getKey() : prefix + "." + field.getKey();

                if (field.getValue().isObject()) {
                    flattenJsonToMap(field.getValue(), key, map);
                } else {
                    map.put(key, field.getValue().asText());
                }
            }
        }
    }

    // Method expected by tests - delegates to executeNotificationAction
    public void sendNotification(NotificationAction action, String message) {
        executeNotificationAction(action, message, null);
    }
}
