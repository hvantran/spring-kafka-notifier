package com.hoatv.kafka.notifier.repository;

import com.hoatv.kafka.notifier.config.TestConfig;
import com.hoatv.kafka.notifier.model.NotificationAction;
import com.hoatv.kafka.notifier.model.NotifierConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@Import(TestConfig.class)
@DisplayName("NotifierConfigurationRepository Tests")
class NotifierConfigurationRepositoryTest {

    @Autowired
    private NotifierConfigurationRepository repository;

    private NotifierConfiguration cpuConfiguration;
    private NotifierConfiguration memoryConfiguration;
    private NotifierConfiguration disabledConfiguration;
    private NotificationAction slackAction;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        slackAction = NotificationAction.builder()
                .type("call")
                .params(Map.of(
                    "provider", "SLACK",
                    "webhookURL", "https://hooks.slack.com/services/xxx/yyy/zzz",
                    "message", "Alert: ${value}"
                ))
                .build();

        cpuConfiguration = NotifierConfiguration.builder()
                .notifier("CpuMonitor")
                .topic("cpu-metrics")
                .rules(Map.of("$gt", Map.of("$field", "value", "$value", 80)))
                .actions(List.of(slackAction))
                .enabled(true)
                .description("CPU usage monitoring")
                .createdAt(LocalDateTime.now())
                .build();

        memoryConfiguration = NotifierConfiguration.builder()
                .notifier("MemoryMonitor")
                .topic("memory-metrics")
                .rules(Map.of("$gt", Map.of("$field", "value", "$value", 90)))
                .actions(List.of(slackAction))
                .enabled(true)
                .description("Memory usage monitoring")
                .createdAt(LocalDateTime.now())
                .build();

        disabledConfiguration = NotifierConfiguration.builder()
                .notifier("DisabledMonitor")
                .topic("disabled-metrics")
                .rules(Map.of("$gt", Map.of("$field", "value", "$value", 70)))
                .actions(List.of(slackAction))
                .enabled(false)
                .description("Disabled monitoring")
                .createdAt(LocalDateTime.now())
                .build();

        repository.saveAll(List.of(cpuConfiguration, memoryConfiguration, disabledConfiguration));
    }

    @Test
    @DisplayName("Should save and retrieve configuration")
    void shouldSaveAndRetrieveConfiguration() {
        // Given
        NotifierConfiguration newConfig = NotifierConfiguration.builder()
                .notifier("DiskMonitor")
                .topic("disk-metrics")
                .rules(Map.of("$lt", Map.of("$field", "freeSpace", "$value", 1000)))
                .actions(List.of(slackAction))
                .enabled(true)
                .description("Disk space monitoring")
                .createdAt(LocalDateTime.now())
                .build();

        // When
        NotifierConfiguration saved = repository.save(newConfig);

        // Then
        assertNotNull(saved.getId());
        assertEquals("DiskMonitor", saved.getNotifier());
        assertEquals("disk-metrics", saved.getTopic());
        assertTrue(saved.isEnabled());
        assertNotNull(saved.getCreatedAt());

        Optional<NotifierConfiguration> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(saved.getNotifier(), found.get().getNotifier());
    }

    @Test
    @DisplayName("Should find configurations by topic")
    void shouldFindConfigurationsByTopic() {
        // When
        List<NotifierConfiguration> cpuConfigs = repository.findByTopic("cpu-metrics");
        List<NotifierConfiguration> memoryConfigs = repository.findByTopic("memory-metrics");
        List<NotifierConfiguration> nonExistentConfigs = repository.findByTopic("non-existent");

        // Then
        assertEquals(1, cpuConfigs.size());
        assertEquals("CpuMonitor", cpuConfigs.get(0).getNotifier());

        assertEquals(1, memoryConfigs.size());
        assertEquals("MemoryMonitor", memoryConfigs.get(0).getNotifier());

        assertEquals(0, nonExistentConfigs.size());
    }

    @Test
    @DisplayName("Should find enabled configurations")
    void shouldFindEnabledConfigurations() {
        // When
        List<NotifierConfiguration> enabledConfigs = repository.findByEnabledTrue();

        // Then
        assertEquals(2, enabledConfigs.size());
        assertTrue(enabledConfigs.stream().allMatch(NotifierConfiguration::isEnabled));
        assertTrue(enabledConfigs.stream().anyMatch(config -> "CpuMonitor".equals(config.getNotifier())));
        assertTrue(enabledConfigs.stream().anyMatch(config -> "MemoryMonitor".equals(config.getNotifier())));
    }

    @Test
    @DisplayName("Should find disabled configurations")
    void shouldFindDisabledConfigurations() {
        // When
        List<NotifierConfiguration> disabledConfigs = repository.findByEnabledFalse();

        // Then
        assertEquals(1, disabledConfigs.size());
        assertFalse(disabledConfigs.get(0).isEnabled());
        assertEquals("DisabledMonitor", disabledConfigs.get(0).getNotifier());
    }

    @Test
    @DisplayName("Should find enabled configurations by topic")
    void shouldFindEnabledConfigurationsByTopic() {
        // Given
        repository.save(NotifierConfiguration.builder()
                .notifier("AnotherCpuMonitor")
                .topic("cpu-metrics")
                .rules(Map.of("$gt", Map.of("$field", "value", "$value", 85)))
                .actions(List.of(slackAction))
                .enabled(false) // Disabled
                .description("Another CPU monitor")
                .createdAt(LocalDateTime.now())
                .build());

        // When
        List<NotifierConfiguration> enabledCpuConfigs = repository.findByTopicAndEnabledTrue("cpu-metrics");
        List<NotifierConfiguration> enabledDisabledConfigs = repository.findByTopicAndEnabledTrue("disabled-metrics");

        // Then
        assertEquals(1, enabledCpuConfigs.size());
        assertEquals("CpuMonitor", enabledCpuConfigs.get(0).getNotifier());
        assertTrue(enabledCpuConfigs.get(0).isEnabled());

        assertEquals(0, enabledDisabledConfigs.size()); // Topic exists but configuration is disabled
    }

    @Test
    @DisplayName("Should check if configuration exists by notifier and topic")
    void shouldCheckIfConfigurationExistsByNotifierAndTopic() {
        // When
        boolean cpuExists = repository.existsByNotifierAndTopic("CpuMonitor", "cpu-metrics");
        boolean memoryExists = repository.existsByNotifierAndTopic("MemoryMonitor", "memory-metrics");
        boolean nonExistentExists = repository.existsByNotifierAndTopic("NonExistent", "non-existent");
        boolean wrongCombinationExists = repository.existsByNotifierAndTopic("CpuMonitor", "memory-metrics");

        // Then
        assertTrue(cpuExists);
        assertTrue(memoryExists);
        assertFalse(nonExistentExists);
        assertFalse(wrongCombinationExists);
    }

    @Test
    @DisplayName("Should support pagination")
    void shouldSupportPagination() {
        // Given
        Pageable firstPage = PageRequest.of(0, 2);
        Pageable secondPage = PageRequest.of(1, 2);

        // When
        Page<NotifierConfiguration> page1 = repository.findAll(firstPage);
        Page<NotifierConfiguration> page2 = repository.findAll(secondPage);

        // Then
        assertEquals(2, page1.getSize());
        assertEquals(2, page1.getContent().size());
        assertEquals(0, page1.getNumber());
        assertEquals(2, page1.getTotalPages());
        assertEquals(3, page1.getTotalElements());
        assertTrue(page1.hasNext());

        assertEquals(2, page2.getSize());
        assertEquals(1, page2.getContent().size()); // Only 1 item on second page
        assertEquals(1, page2.getNumber());
        assertEquals(2, page2.getTotalPages());
        assertEquals(3, page2.getTotalElements());
        assertFalse(page2.hasNext());
    }

    @Test
    @DisplayName("Should update configuration")
    void shouldUpdateConfiguration() {
        // Given
        NotifierConfiguration config = repository.findByTopic("cpu-metrics").get(0);
        String originalId = config.getId();
        LocalDateTime originalCreatedAt = config.getCreatedAt();

        // When
        config.setDescription("Updated CPU monitoring");
        config.setEnabled(false);
        config.setUpdatedAt(LocalDateTime.now());
        NotifierConfiguration updated = repository.save(config);

        // Then
        assertEquals(originalId, updated.getId()); // ID should not change
        assertEquals(originalCreatedAt, updated.getCreatedAt()); // CreatedAt should not change
        assertEquals("Updated CPU monitoring", updated.getDescription());
        assertFalse(updated.isEnabled());
        assertNotNull(updated.getUpdatedAt());
    }

    @Test
    @DisplayName("Should delete configuration")
    void shouldDeleteConfiguration() {
        // Given
        NotifierConfiguration config = repository.findByTopic("cpu-metrics").get(0);
        String configId = config.getId();

        // When
        repository.delete(config);

        // Then
        Optional<NotifierConfiguration> found = repository.findById(configId);
        assertFalse(found.isPresent());

        List<NotifierConfiguration> cpuConfigs = repository.findByTopic("cpu-metrics");
        assertEquals(0, cpuConfigs.size());
    }

    @Test
    @DisplayName("Should find configurations by notifier")
    void shouldFindConfigurationsByNotifier() {
        // Given
        repository.save(NotifierConfiguration.builder()
                .notifier("CpuMonitor") // Same notifier, different topic
                .topic("cpu-detailed-metrics")
                .rules(Map.of("$gt", Map.of("$field", "detailedValue", "$value", 75)))
                .actions(List.of(slackAction))
                .enabled(true)
                .description("Detailed CPU monitoring")
                .createdAt(LocalDateTime.now())
                .build());

        // When
        List<NotifierConfiguration> cpuMonitorConfigs = repository.findByNotifier("CpuMonitor");
        List<NotifierConfiguration> memoryMonitorConfigs = repository.findByNotifier("MemoryMonitor");

        // Then
        assertEquals(2, cpuMonitorConfigs.size());
        assertTrue(cpuMonitorConfigs.stream().allMatch(config -> "CpuMonitor".equals(config.getNotifier())));

        assertEquals(1, memoryMonitorConfigs.size());
        assertEquals("MemoryMonitor", memoryMonitorConfigs.get(0).getNotifier());
    }

    @Test
    @DisplayName("Should handle case-sensitive queries")
    void shouldHandleCaseSensitiveQueries() {
        // When
        List<NotifierConfiguration> upperCaseResult = repository.findByTopic("CPU-METRICS");
        List<NotifierConfiguration> correctCaseResult = repository.findByTopic("cpu-metrics");

        // Then
        assertEquals(0, upperCaseResult.size()); // Case sensitive - should not match
        assertEquals(1, correctCaseResult.size()); // Exact case - should match
    }

    @Test
    @DisplayName("Should handle null and empty values")
    void shouldHandleNullAndEmptyValues() {
        // When
        List<NotifierConfiguration> nullTopicResult = repository.findByTopic(null);
        List<NotifierConfiguration> emptyTopicResult = repository.findByTopic("");

        // Then
        assertEquals(0, nullTopicResult.size());
        assertEquals(0, emptyTopicResult.size());
    }

    @Test
    @DisplayName("Should maintain data integrity with complex rules and actions")
    void shouldMaintainDataIntegrityWithComplexRulesAndActions() {
        // Given
        Map<String, Object> complexRules = Map.of(
            "$and", List.of(
                Map.of("$gt", Map.of("$field", "cpu", "$value", 80)),
                Map.of("$lt", Map.of("$field", "memory", "$value", 90))
            )
        );

        NotificationAction slackAction1 = NotificationAction.builder()
                .type("call")
                .params(Map.of("provider", "SLACK", "webhookURL", "https://slack.com/webhook1"))
                .build();

        NotificationAction slackAction2 = NotificationAction.builder()
                .type("call")
                .params(Map.of("provider", "SLACK", "webhookURL", "https://slack.com/webhook2"))
                .build();

        NotifierConfiguration complexConfig = NotifierConfiguration.builder()
                .notifier("ComplexMonitor")
                .topic("complex-metrics")
                .rules(complexRules)
                .actions(List.of(slackAction1, slackAction2))
                .enabled(true)
                .description("Complex monitoring with multiple actions")
                .createdAt(LocalDateTime.now())
                .build();

        // When
        NotifierConfiguration saved = repository.save(complexConfig);
        Optional<NotifierConfiguration> retrieved = repository.findById(saved.getId());

        // Then
        assertTrue(retrieved.isPresent());
        NotifierConfiguration config = retrieved.get();

        assertEquals("ComplexMonitor", config.getNotifier());
        assertEquals("complex-metrics", config.getTopic());
        assertEquals(complexRules, config.getRules());
        assertEquals(2, config.getActions().size());
        assertTrue(config.getActions().stream().allMatch(action -> "call".equals(action.getType())));
        assertTrue(config.getActions().stream().allMatch(action -> "SLACK".equals(action.getParams().get("provider"))));
    }
}
