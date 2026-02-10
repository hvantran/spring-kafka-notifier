package com.hoatv.kafka.notifier.controller;

import com.hoatv.kafka.notifier.service.DynamicKafkaMessageProcessor;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/kafka")
public class KafkaTopicController {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaTopicController.class);

    private final DynamicKafkaMessageProcessor dynamicProcessor;

    @Operation(summary = "Get currently subscribed topics")
    @ApiResponse(responseCode = "200", description = "Currently subscribed topics retrieved successfully")
    @GetMapping("/subscriptions")
    public ResponseEntity<Set<String>> getSubscribedTopics() {
        LOGGER.debug("Getting currently subscribed Kafka topics");
        Set<String> subscribedTopics = dynamicProcessor.getSubscribedTopics();
        return ResponseEntity.ok(subscribedTopics);
    }

    @Operation(summary = "Check if subscribed to a specific topic")
    @ApiResponse(responseCode = "200", description = "Subscription status retrieved successfully")
    @GetMapping("/subscriptions/{topic}")
    public ResponseEntity<Map<String, Object>> isSubscribedToTopic(@PathVariable("topic") String topic) {
        LOGGER.debug("Checking subscription status for topic: {}", topic);
        boolean isSubscribed = dynamicProcessor.isSubscribedToTopic(topic);

        Map<String, Object> response = Map.of(
                "topic", topic,
                "subscribed", isSubscribed
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Refresh all topic subscriptions")
    @ApiResponse(responseCode = "200", description = "Topic subscriptions refreshed successfully")
    @PostMapping("/subscriptions:sync")
    public ResponseEntity<Map<String, Object>> refreshSubscriptions() {
        LOGGER.info("Manual refresh of Kafka topic subscriptions requested");

        Set<String> beforeRefresh = dynamicProcessor.getSubscribedTopics();
        dynamicProcessor.refreshTopicSubscriptions();
        Set<String> afterRefresh = dynamicProcessor.getSubscribedTopics();

        Map<String, Object> response = Map.of(
                "message", "Topic subscriptions refreshed successfully",
                "beforeRefresh", beforeRefresh,
                "afterRefresh", afterRefresh
        );

        LOGGER.info("Topic subscriptions manually refreshed. Before: {}, After: {}",
                beforeRefresh, afterRefresh);

        return ResponseEntity.ok(response);
    }
}
