package com.hoatv.kafka.notifier.service;

import com.hoatv.fwk.common.exceptions.DuplicateResourceException;
import com.hoatv.fwk.common.exceptions.EntityNotFoundException;
import com.hoatv.kafka.notifier.dto.NotifierConfigurationRequest;
import com.hoatv.kafka.notifier.dto.NotifierConfigurationResponse;
import com.hoatv.kafka.notifier.model.NotifierConfiguration;
import com.hoatv.kafka.notifier.repository.NotifierConfigurationRepository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotifierConfigurationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifierConfigurationService.class);

    private final KafkaService kafkaService;
    private final NotifierConfigurationRepository repository;

    public NotifierConfigurationResponse create(NotifierConfigurationRequest request) {
        LOGGER.info("Creating notifier configuration for notifier: {}, topic: {}",
                request.getNotifier(), request.getTopic());

        if (repository.existsByNotifierAndTopic(request.getNotifier(), request.getTopic())) {
            throw new DuplicateResourceException(
                    String.format("Configuration already exists for notifier '%s' and topic '%s'",
                            request.getNotifier(), request.getTopic()));
        }

        NotifierConfiguration config = NotifierConfiguration.builder()
                .notifier(request.getNotifier())
                .topic(request.getTopic())
                .rules(request.getRules())
                .actions(request.getActions())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .description(request.getDescription())
                .throttlePeriodMinutes(request.getThrottlePeriodMinutes())
                .throttlePermitsPerPeriod(request.getThrottlePermitsPerPeriod())
                .createdAt(LocalDateTime.now())
                .build();

        NotifierConfiguration savedNotifierConfiguration = repository.save(config);
        LOGGER.info("Successfully created notifier configuration with ID: {}", savedNotifierConfiguration.getId());
        if (savedNotifierConfiguration.isEnabled()) {
            kafkaService.addTopicSubscription(savedNotifierConfiguration.getTopic());
        }

        return mapToResponse(savedNotifierConfiguration);
    }

    public NotifierConfigurationResponse update(String id, NotifierConfigurationRequest request) {
        LOGGER.info("Updating notifier configuration with ID: {}", id);

        NotifierConfiguration existingConfig = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("NotifierConfiguration not found with ID: " + id));

        // Check if updating notifier/topic combination conflicts with existing config
        String topic = existingConfig.getTopic();
        if (!existingConfig.getNotifier().equals(request.getNotifier()) ||
                !topic.equals(request.getTopic())) {
            if (repository.existsByNotifierAndTopic(request.getNotifier(), request.getTopic())) {
                throw new DuplicateResourceException(
                        String.format("Configuration already exists for notifier '%s' and topic '%s'",
                                request.getNotifier(), request.getTopic()));
            }
        }

        existingConfig.setNotifier(request.getNotifier());
        existingConfig.setTopic(request.getTopic());
        existingConfig.setRules(request.getRules());
        existingConfig.setActions(request.getActions());
        existingConfig.setEnabled(request.getEnabled() != null ? request.getEnabled() : existingConfig.isEnabled());
        existingConfig.setDescription(request.getDescription());
        existingConfig.setThrottlePeriodMinutes(request.getThrottlePeriodMinutes());
        existingConfig.setThrottlePermitsPerPeriod(request.getThrottlePermitsPerPeriod());
        existingConfig.setUpdatedAt(LocalDateTime.now());

        NotifierConfiguration updated = repository.save(existingConfig);
        LOGGER.info("Successfully updated notifier configuration with ID: {}", id);

        // Handle topic subscription changes
        if (updated.isEnabled()) {
            kafkaService.addTopicSubscription(updated.getTopic());
        }
        // Check if old topic still has enabled configurations
        if (!topic.equals(updated.getTopic())) {
            List<NotifierConfiguration> enabledConfigs = findEnabledConfigurationsByTopic(topic);
            kafkaService.removeTopicSubscriptionIfUnused(topic, enabledConfigs);
        }

        return mapToResponse(updated);
    }

    public NotifierConfigurationResponse findById(String id) {
        LOGGER.debug("Finding notifier configuration with ID: {}", id);

        NotifierConfiguration config = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "NotifierConfiguration not found with ID: " + id));

        return mapToResponse(config);
    }

    public Page<NotifierConfigurationResponse> findAll(Pageable pageable) {
        LOGGER.debug("Finding all notifier configurations with pagination");

        return repository.findAll(pageable)
                .map(this::mapToResponse);
    }

    public List<NotifierConfigurationResponse> findByTopic(String topic) {
        LOGGER.debug("Finding notifier configurations for topic: {}", topic);

        return repository.findByTopic(topic).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<NotifierConfigurationResponse> findEnabledConfigurations() {
        LOGGER.debug("Finding all enabled notifier configurations");

        return repository.findByEnabledTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<NotifierConfiguration> findEnabledConfigurationsByTopic(String topic) {
        LOGGER.debug("Finding enabled notifier configurations for topic: {}", topic);
        return repository.findByTopicAndEnabledTrue(topic);
    }

    public void delete(String id) {
        LOGGER.info("Deleting notifier configuration with ID: {}", id);
        NotifierConfiguration config = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "NotifierConfiguration not found with ID: " + id));

        String topic = config.getTopic();
        repository.deleteById(id);

        List<NotifierConfiguration> enabledConfigs = findEnabledConfigurationsByTopic(topic);
        kafkaService.removeTopicSubscriptionIfUnused(topic, enabledConfigs);
        LOGGER.info("Successfully deleted notifier configuration with ID: {}", id);
    }

    public NotifierConfigurationResponse toggleEnabled(String id) {
        LOGGER.info("Toggling enabled status for notifier configuration with ID: {}", id);

        NotifierConfiguration config = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "NotifierConfiguration not found with ID: " + id));

        config.setEnabled(!config.isEnabled());
        config.setUpdatedAt(LocalDateTime.now());

        NotifierConfiguration updated = repository.save(config);
        LOGGER.info("Successfully toggled enabled status for notifier configuration with ID: {} to {}",
                id, updated.isEnabled());

        // Handle topic subscription based on new enabled status
        String updatedTopic = updated.getTopic();
        if (updated.isEnabled()) {
            kafkaService.addTopicSubscription(updatedTopic);
        } else {
            List<NotifierConfiguration> enabledConfigs = findEnabledConfigurationsByTopic(updatedTopic);
            kafkaService.removeTopicSubscriptionIfUnused(updatedTopic, enabledConfigs);
        }

        return mapToResponse(updated);
    }

    private NotifierConfigurationResponse mapToResponse(NotifierConfiguration config) {
        return NotifierConfigurationResponse.builder()
                .id(config.getId())
                .notifier(config.getNotifier())
                .topic(config.getTopic())
                .rules(config.getRules())
                .actions(config.getActions())
                .enabled(config.isEnabled())
                .description(config.getDescription())
                .throttlePeriodMinutes(config.getThrottlePeriodMinutes())
                .throttlePermitsPerPeriod(config.getThrottlePermitsPerPeriod())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .createdBy(config.getCreatedBy())
                .updatedBy(config.getUpdatedBy())
                .build();
    }
}
