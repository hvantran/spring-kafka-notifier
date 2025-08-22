package com.hoatv.kafka.notifier.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Simplified Dynamic Kafka Message Processor
 * 
 * This class demonstrates how to dynamically subscribe/unsubscribe from Kafka topics
 * at runtime without requiring application restart.
 * 
 * Key improvements over static @KafkaListener:
 * 1. Runtime topic subscription management
 * 2. Immediate response to configuration changes
 * 3. Resource-efficient topic management
 * 4. Zero-downtime topic management
 */
@Service
@RequiredArgsConstructor
public class SimpleDynamicKafkaProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDynamicKafkaProcessor.class);

    // Inject the consumer factory (configured via application properties)
    private final ConsumerFactory<String, String> consumerFactory;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    // Thread-safe collections to track active subscriptions
    private final Set<String> subscribedTopics = new CopyOnWriteArraySet<>();
    private final Map<String, KafkaMessageListenerContainer<String, String>> topicContainers = new ConcurrentHashMap<>();

    @PostConstruct
    public void initializeSubscriptions() {
        LOGGER.info("Dynamic Kafka processor initialized");
        // You can initialize with existing topics here
        // Example: subscribeToTopic("default-topic");
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
     * Subscribe to a new topic at runtime
     * This method can be called when a new notifier configuration is created
     */
    public void subscribeToTopic(String topic) {
        if (subscribedTopics.contains(topic)) {
            LOGGER.debug("Already subscribed to topic: {}", topic);
            return;
        }

        try {
            LOGGER.info("Subscribing to topic: {}", topic);

            // Create container properties for the topic
            ContainerProperties containerProps = new ContainerProperties(topic);
            containerProps.setGroupId(groupId);
            
            // Set message listener - this is where messages are processed
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
     * Unsubscribe from a topic at runtime
     * This method can be called when a notifier configuration is deleted
     */
    public void unsubscribeFromTopic(String topic) {
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
     * Process incoming Kafka message
     * This is where your business logic goes
     */
    private void processMessage(String message, String topic) {
        LOGGER.info("Processing message from topic '{}': {}", topic, message);
        
        // TODO: Add your message processing logic here
        // For example:
        // 1. Parse the message
        // 2. Evaluate rules/conditions 
        // 3. Send notifications if conditions are met
        
        try {
            // Simulate message processing
            LOGGER.debug("Message processed successfully for topic: {}", topic);
        } catch (Exception e) {
            LOGGER.error("Error processing message from topic '{}': {}", topic, e.getMessage(), e);
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

    /**
     * Example method showing how to integrate with configuration changes
     * This should be called when a new notifier configuration is created
     */
    public void onNotifierConfigurationCreated(String topic, boolean enabled) {
        if (enabled) {
            LOGGER.info("New notifier configuration created for topic '{}' - subscribing", topic);
            subscribeToTopic(topic);
        }
    }

    /**
     * Example method showing how to handle configuration deletion
     * This should be called when a notifier configuration is deleted
     */
    public void onNotifierConfigurationDeleted(String topic) {
        // Check if there are other enabled configurations for this topic
        // If not, unsubscribe from the topic
        LOGGER.info("Notifier configuration deleted for topic '{}' - checking if unsubscribe needed", topic);
        
        // TODO: Check if other configurations exist for this topic
        // For now, just unsubscribe (you'd implement proper logic here)
        unsubscribeFromTopic(topic);
    }
}
