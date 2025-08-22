package com.hoatv.kafka.notifier.client;

import com.hoatv.kafka.notifier.dto.SlackMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client for sending messages to Slack via webhook URLs
 */
@Component
public class SlackWebhookClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlackWebhookClient.class);
    
    private final RestTemplate restTemplate;

    public SlackWebhookClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Send a message to Slack via webhook
     *
     * @param webhookUrl The Slack webhook URL
     * @param message The message to send
     */
    public void sendMessage(String webhookUrl, SlackMessage message) {
        try {
            LOGGER.info("Sending Slack notification to: {}", webhookUrl);
            restTemplate.postForObject(webhookUrl, message, String.class);
            LOGGER.info("Slack notification sent successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to send Slack notification: {}", e.getMessage(), e);
        }
    }
}
