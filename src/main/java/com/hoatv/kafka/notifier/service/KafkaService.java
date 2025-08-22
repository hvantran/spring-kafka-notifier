package com.hoatv.kafka.notifier.service;

import com.hoatv.kafka.notifier.model.NotifierConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KafkaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaService.class);

    private final DynamicKafkaMessageProcessor dynamicKafkaMessageProcessor;

    public KafkaService(DynamicKafkaMessageProcessor dynamicKafkaMessageProcessor) {
        this.dynamicKafkaMessageProcessor = dynamicKafkaMessageProcessor;
    }

    /**
     * Add a new topic subscription immediately (called when new notifier is created)
     */
    public void addTopicSubscription(String topic) {
        LOGGER.info("Adding immediate subscription for new topic: {}", topic);
        dynamicKafkaMessageProcessor.subscribeToTopic(topic);
    }

    /**
     * Remove topic subscription if no enabled configurations exist
     */
    public void removeTopicSubscriptionIfUnused(String topic, List<NotifierConfiguration> enabledConfigs) {
        if (enabledConfigs.isEmpty()) {
            LOGGER.info("No enabled configurations found for topic '{}', removing subscription", topic);
            dynamicKafkaMessageProcessor.unsubscribeFromTopic(topic);
        }
    }
}
