package com.hoatv.kafka.notifier.controller;

import com.hoatv.kafka.notifier.service.SimpleDynamicKafkaProcessor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@Tag(name = "Dynamic Kafka Demo", description = "API to demonstrate dynamic Kafka topic management")
@RestController
@RequestMapping("/api/v1/kafka-demo")
@RequiredArgsConstructor
public class DynamicKafkaDemoController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicKafkaDemoController.class);

    private final SimpleDynamicKafkaProcessor dynamicProcessor;

    @Operation(summary = "Get currently subscribed topics")
    @ApiResponse(responseCode = "200", description = "Currently subscribed topics retrieved successfully")
    @GetMapping("/subscriptions")
    public ResponseEntity<Set<String>> getSubscribedTopics() {
        LOGGER.info("Getting currently subscribed Kafka topics");
        Set<String> subscribedTopics = dynamicProcessor.getSubscribedTopics();
        return ResponseEntity.ok(subscribedTopics);
    }

    @Operation(summary = "Subscribe to a new topic dynamically")
    @ApiResponse(responseCode = "200", description = "Successfully subscribed to topic")
    @PostMapping("/subscriptions/{topic}")
    public ResponseEntity<Map<String, Object>> subscribeToTopic(@PathVariable String topic) {
        LOGGER.info("Request to subscribe to topic: {}", topic);
        
        try {
            dynamicProcessor.subscribeToTopic(topic);
            
            Map<String, Object> response = Map.of(
                "message", "Successfully subscribed to topic: " + topic,
                "topic", topic,
                "subscribed", true
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            LOGGER.error("Failed to subscribe to topic '{}': {}", topic, e.getMessage(), e);
            
            Map<String, Object> response = Map.of(
                "message", "Failed to subscribe to topic: " + topic,
                "topic", topic,
                "subscribed", false,
                "error", e.getMessage()
            );
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @Operation(summary = "Unsubscribe from a topic dynamically")
    @ApiResponse(responseCode = "200", description = "Successfully unsubscribed from topic")
    @DeleteMapping("/subscriptions/{topic}")
    public ResponseEntity<Map<String, Object>> unsubscribeFromTopic(@PathVariable String topic) {
        LOGGER.info("Request to unsubscribe from topic: {}", topic);
        
        try {
            dynamicProcessor.unsubscribeFromTopic(topic);
            
            Map<String, Object> response = Map.of(
                "message", "Successfully unsubscribed from topic: " + topic,
                "topic", topic,
                "subscribed", false
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            LOGGER.error("Failed to unsubscribe from topic '{}': {}", topic, e.getMessage(), e);
            
            Map<String, Object> response = Map.of(
                "message", "Failed to unsubscribe from topic: " + topic,
                "topic", topic,
                "error", e.getMessage()
            );
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @Operation(summary = "Check if subscribed to a specific topic")
    @ApiResponse(responseCode = "200", description = "Subscription status retrieved successfully")
    @GetMapping("/subscriptions/{topic}")
    public ResponseEntity<Map<String, Object>> isSubscribedToTopic(@PathVariable String topic) {
        LOGGER.info("Checking subscription status for topic: {}", topic);
        boolean isSubscribed = dynamicProcessor.isSubscribedToTopic(topic);
        
        Map<String, Object> response = Map.of(
            "topic", topic,
            "subscribed", isSubscribed
        );
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Simulate creating a new notifier configuration")
    @ApiResponse(responseCode = "200", description = "Notifier configuration simulation completed")
    @PostMapping("/demo/create-notifier")
    public ResponseEntity<Map<String, Object>> simulateCreateNotifier(
            @RequestParam String topic,
            @RequestParam(defaultValue = "true") boolean enabled) {
        
        LOGGER.info("Simulating creation of notifier for topic: {}, enabled: {}", topic, enabled);
        
        // This simulates what would happen when a real notifier configuration is created
        dynamicProcessor.onNotifierConfigurationCreated(topic, enabled);
        
        Map<String, Object> response = Map.of(
            "message", "Simulated notifier creation for topic: " + topic,
            "topic", topic,
            "enabled", enabled,
            "subscribed", dynamicProcessor.isSubscribedToTopic(topic)
        );
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Simulate deleting a notifier configuration")
    @ApiResponse(responseCode = "200", description = "Notifier deletion simulation completed")
    @DeleteMapping("/demo/delete-notifier/{topic}")
    public ResponseEntity<Map<String, Object>> simulateDeleteNotifier(@PathVariable String topic) {
        
        LOGGER.info("Simulating deletion of notifier for topic: {}", topic);
        
        // This simulates what would happen when a notifier configuration is deleted
        dynamicProcessor.onNotifierConfigurationDeleted(topic);
        
        Map<String, Object> response = Map.of(
            "message", "Simulated notifier deletion for topic: " + topic,
            "topic", topic,
            "subscribed", dynamicProcessor.isSubscribedToTopic(topic)
        );
        
        return ResponseEntity.ok(response);
    }
}
