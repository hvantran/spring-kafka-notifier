package com.hoatv.kafka.notifier.service;

import com.hoatv.kafka.notifier.dto.NotifierConfigurationResponse;
import com.hoatv.kafka.notifier.model.NotifierConfiguration;
import com.hoatv.kafka.notifier.model.NotificationAction;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DynamicKafkaMessageProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicKafkaMessageProcessor.class);

    private final NotifierConfigurationService configurationService;
    private final RuleEvaluationService ruleEvaluationService;
    private final NotificationService notificationService;
    @Qualifier("dynamicConsumerFactory")
    private final ConsumerFactory<String, String> consumerFactory;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    // Track active topic subscriptions
    private final Set<String> subscribedTopics = new CopyOnWriteArraySet<>();
    private final Map<String, KafkaMessageListenerContainer<String, String>> topicContainers = new ConcurrentHashMap<>();

    @PostConstruct
    public void initializeSubscriptions() {
        LOGGER.info("Initializing dynamic Kafka subscriptions");
        refreshTopicSubscriptions();
    }

    @PreDestroy
    public void cleanup() {
        LOGGER.info("Cleaning up Kafka listeners");
        topicContainers.values().forEach(container -> {
            if (container.isRunning()) {
                container.stop();
            }
        });
        topicContainers.clear();
        subscribedTopics.clear();
    }

    /**
     * Refresh topic subscriptions based on current enabled configurations
     * This method should be called whenever a new notifier configuration is added
     */
    public void refreshTopicSubscriptions() {
        LOGGER.info("Refreshing Kafka topic subscriptions");
        
        try {
            // Get all topics from enabled configurations
            Set<String> requiredTopics = configurationService.findEnabledConfigurations()
                    .stream()
                    .map(NotifierConfigurationResponse::getTopic)
                    .collect(Collectors.toSet());

            LOGGER.debug("Required topics: {}, Currently subscribed: {}", requiredTopics, subscribedTopics);

            // Remove subscriptions for topics that are no longer needed
            Set<String> topicsToUnsubscribe = new CopyOnWriteArraySet<>(subscribedTopics);
            topicsToUnsubscribe.removeAll(requiredTopics);
            
            for (String topic : topicsToUnsubscribe) {
                unsubscribeFromTopic(topic);
            }

            // Add subscriptions for new topics
            Set<String> topicsToSubscribe = new CopyOnWriteArraySet<>(requiredTopics);
            topicsToSubscribe.removeAll(subscribedTopics);
            
            for (String topic : topicsToSubscribe) {
                subscribeToTopic(topic);
            }

            LOGGER.info("Topic subscription refresh completed. Active topics: {}", subscribedTopics);

        } catch (Exception e) {
            LOGGER.error("Error refreshing topic subscriptions: {}", e.getMessage(), e);
        }
    }

    /**
     * Subscribe to a specific topic
     */
    private void subscribeToTopic(String topic) {
        if (subscribedTopics.contains(topic)) {
            LOGGER.debug("Already subscribed to topic: {}", topic);
            return;
        }

        try {
            LOGGER.info("Subscribing to topic: {}", topic);

            // Create container properties
            ContainerProperties containerProps = new ContainerProperties(topic);
            containerProps.setGroupId(groupId);
            containerProps.setMessageListener((MessageListener<String, String>) record -> {
                processMessage(record.value(), record.topic());
            });

            // Create and start the container
            KafkaMessageListenerContainer<String, String> container = 
                new KafkaMessageListenerContainer<>(consumerFactory, containerProps);
            container.start();

            // Track the subscription
            topicContainers.put(topic, container);
            subscribedTopics.add(topic);

            LOGGER.info("Successfully subscribed to topic: {}", topic);

        } catch (Exception e) {
            LOGGER.error("Failed to subscribe to topic '{}': {}", topic, e.getMessage(), e);
        }
    }

    /**
     * Unsubscribe from a specific topic
     */
    private void unsubscribeFromTopic(String topic) {
        if (!subscribedTopics.contains(topic)) {
            LOGGER.debug("Not subscribed to topic: {}", topic);
            return;
        }

        try {
            LOGGER.info("Unsubscribing from topic: {}", topic);

            KafkaMessageListenerContainer<String, String> container = topicContainers.get(topic);
            if (container != null && container.isRunning()) {
                container.stop();
            }

            topicContainers.remove(topic);
            subscribedTopics.remove(topic);

            LOGGER.info("Successfully unsubscribed from topic: {}", topic);

        } catch (Exception e) {
            LOGGER.error("Failed to unsubscribe from topic '{}': {}", topic, e.getMessage(), e);
        }
    }

    /**
     * Add a new topic subscription immediately (called when new notifier is created)
     */
    public void addTopicSubscription(String topic) {
        LOGGER.info("Adding immediate subscription for new topic: {}", topic);
        subscribeToTopic(topic);
    }

    /**
     * Remove topic subscription if no enabled configurations exist
     */
    public void removeTopicSubscriptionIfUnused(String topic) {
        List<NotifierConfiguration> enabledConfigs = configurationService.findEnabledConfigurationsByTopic(topic);
        if (enabledConfigs.isEmpty()) {
            LOGGER.info("No enabled configurations found for topic '{}', removing subscription", topic);
            unsubscribeFromTopic(topic);
        }
    }

    /**
     * Process incoming Kafka message (same logic as the original processor)
     */
    public void processMessage(String message, String topic) {
        LOGGER.debug("Received message from topic '{}': {}", topic, message);

        try {
            List<NotifierConfiguration> configurations =
                    configurationService.findEnabledConfigurationsByTopic(topic);

            if (configurations.isEmpty()) {
                LOGGER.debug("No enabled configurations found for topic: {}", topic);
                return;
            }

            LOGGER.info("Processing {} configurations for topic: {}", configurations.size(), topic);
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

            boolean rulesMatch = ruleEvaluationService.evaluateRules(
                    config.getRules(), message);

            if (rulesMatch) {
                LOGGER.info("Rules matched for configuration: {} on topic: {}. Executing actions.", 
                           config.getNotifier(), topic);
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

            if (action.getType().equalsIgnoreCase("call")) {
                notificationService.executeNotificationAction(action, message, config);
            } else {
                LOGGER.warn("Unsupported action type: {} for configuration: {}",
                        action.getType(), config.getNotifier());
            }

        } catch (Exception e) {
            LOGGER.error("Error executing action of type '{}' for configuration '{}': {}",
                    action.getType(), config.getNotifier(), e.getMessage(), e);
        }
    }

    /**
     * Get currently subscribed topics (for monitoring/debugging)
     */
    public Set<String> getSubscribedTopics() {
        return new CopyOnWriteArraySet<>(subscribedTopics);
    }

    /**
     * Check if subscribed to a specific topic
     */
    public boolean isSubscribedToTopic(String topic) {
        return subscribedTopics.contains(topic);
    }
}
