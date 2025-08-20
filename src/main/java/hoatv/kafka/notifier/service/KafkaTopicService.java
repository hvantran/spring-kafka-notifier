package hoatv.kafka.notifier.service;

import hoatv.kafka.notifier.model.NotifierConfiguration;
import hoatv.kafka.notifier.repository.NotifierConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service("kafkaTopicService")
@RequiredArgsConstructor
public class KafkaTopicService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaTopicService.class);
    
    private final NotifierConfigurationRepository repository;
    
    /**
     * Get all unique topics from enabled notifier configurations
     * This is used by the Kafka listener to dynamically subscribe to topics
     */
    public String[] getAllTopics() {
        try {
            List<String> topics = repository.findByEnabledTrue()
                    .stream()
                    .map(NotifierConfiguration::getTopic)
                    .distinct()
                    .collect(Collectors.toList());
            
            if (topics.isEmpty()) {
                LOGGER.warn("No enabled notifier configurations found, returning default topic");
                return new String[]{"default-topic"};
            }
            
            LOGGER.debug("Found {} unique topics from enabled configurations: {}", 
                    topics.size(), topics);
            
            return topics.toArray(new String[0]);
            
        } catch (Exception e) {
            LOGGER.error("Error getting topics from configurations: {}", e.getMessage(), e);
            return new String[]{"default-topic"};
        }
    }
    
    /**
     * Get all unique topics from all notifier configurations (enabled and disabled)
     */
    public List<String> getAllTopicsIncludingDisabled() {
        try {
            return repository.findAll()
                    .stream()
                    .map(NotifierConfiguration::getTopic)
                    .distinct()
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            LOGGER.error("Error getting all topics from configurations: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * Check if a topic has any enabled configurations
     */
    public boolean hasEnabledConfigurations(String topic) {
        try {
            return !repository.findByTopicAndEnabledTrue(topic).isEmpty();
        } catch (Exception e) {
            LOGGER.error("Error checking enabled configurations for topic '{}': {}", 
                    topic, e.getMessage(), e);
            return false;
        }
    }
}
