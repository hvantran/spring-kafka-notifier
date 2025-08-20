/*
package hoatv.kafka.notifier.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import hoatv.kafka.notifier.SpringKafkaNotifierApplication;
import hoatv.kafka.notifier.dto.NotifierConfigurationRequest;
import hoatv.kafka.notifier.dto.NotifierConfigurationResponse;
import hoatv.kafka.notifier.model.NotificationAction;
import hoatv.kafka.notifier.repository.NotifierConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = SpringKafkaNotifierApplication.class)
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "spring.data.mongodb.host=localhost",
    "spring.data.mongodb.port=0",
    "spring.kafka.bootstrap-servers=localhost:9092",
    "kafka.topics=test-topic"
})
@DisplayName("Integration Tests - Full Application Flow")
class NotifierConfigurationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotifierConfigurationRepository repository;

    private NotifierConfigurationRequest validRequest;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        NotificationAction slackAction = NotificationAction.builder()
                .type("call")
                .params(Map.of(
                    "provider", "SLACK",
                    "webhookURL", "https://hooks.slack.com/services/xxx/yyy/zzz",
                    "message", "CPU usage high: ${value}"
                ))
                .build();

        validRequest = NotifierConfigurationRequest.builder()
                .notifier("IntegrationTestNotifier")
                .topic("integration-test-topic")
                .rules(Map.of("$gt", Map.of("$field", "value", "$value", 80)))
                .actions(List.of(slackAction))
                .enabled(true)
                .description("Integration test configuration")
                .build();
    }

    @Test
    @DisplayName("Should perform complete CRUD operations flow")
    void shouldPerformCompleteCrudOperationsFlow() throws Exception {
        // 1. Create configuration
        String createResponse = mockMvc.perform(post("/api/v1/configurations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notifier").value("IntegrationTestNotifier"))
                .andExpect(jsonPath("$.topic").value("integration-test-topic"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        NotifierConfigurationResponse created = objectMapper.readValue(createResponse, NotifierConfigurationResponse.class);
        String configId = created.getId();

        // 2. Get configuration by ID
        mockMvc.perform(get("/api/v1/configurations/{id}", configId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(configId))
                .andExpect(jsonPath("$.notifier").value("IntegrationTestNotifier"))
                .andExpect(jsonPath("$.topic").value("integration-test-topic"));

        // 3. Get all configurations
        mockMvc.perform(get("/api/v1/configurations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(configId));

        // 4. Get configurations by topic
        mockMvc.perform(get("/api/v1/configurations/topic/{topic}", "integration-test-topic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(configId));

        // 5. Get enabled configurations
        mockMvc.perform(get("/api/v1/configurations/enabled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].enabled").value(true));

        // 6. Update configuration
        validRequest.setDescription("Updated integration test configuration");
        mockMvc.perform(put("/api/v1/configurations/{id}", configId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(configId))
                .andExpect(jsonPath("$.description").value("Updated integration test configuration"));

        // 7. Toggle enabled status
        mockMvc.perform(patch("/api/v1/configurations/{id}/toggle", configId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(configId))
                .andExpect(jsonPath("$.enabled").value(false));

        // 8. Verify configuration is no longer in enabled list
        mockMvc.perform(get("/api/v1/configurations/enabled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 9. Delete configuration
        mockMvc.perform(delete("/api/v1/configurations/{id}", configId))
                .andExpect(status().isNoContent());

        // 10. Verify configuration is deleted
        mockMvc.perform(get("/api/v1/configurations/{id}", configId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should handle duplicate configuration creation")
    void shouldHandleDuplicateConfigurationCreation() throws Exception {
        // 1. Create first configuration
        mockMvc.perform(post("/api/v1/configurations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        // 2. Try to create duplicate configuration (same notifier + topic)
        mockMvc.perform(post("/api/v1/configurations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Configuration already exists")));
    }

    @Test
    @DisplayName("Should handle validation errors")
    void shouldHandleValidationErrors() throws Exception {
        // Create invalid request (missing required fields)
        NotifierConfigurationRequest invalidRequest = NotifierConfigurationRequest.builder()
                .notifier("") // Empty notifier
                .topic("test-topic")
                .rules(Map.of())
                .actions(List.of())
                .build();

        mockMvc.perform(post("/api/v1/configurations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle pagination correctly")
    void shouldHandlePaginationCorrectly() throws Exception {
        // Create multiple configurations
        for (int i = 0; i < 5; i++) {
            NotifierConfigurationRequest request = NotifierConfigurationRequest.builder()
                    .notifier("TestNotifier" + i)
                    .topic("test-topic-" + i)
                    .rules(Map.of("$gt", Map.of("$field", "value", "$value", 80 + i)))
                    .actions(List.of(NotificationAction.builder()
                            .type("call")
                            .params(Map.of("provider", "SLACK", "webhookURL", "https://slack.com/webhook" + i))
                            .build()))
                    .enabled(true)
                    .description("Test configuration " + i)
                    .build();

            mockMvc.perform(post("/api/v1/configurations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Test pagination
        mockMvc.perform(get("/api/v1/configurations")
                .param("page", "0")
                .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(3));

        mockMvc.perform(get("/api/v1/configurations")
                .param("page", "1")
                .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(3));
    }

    @Test
    @DisplayName("Should filter configurations by topic correctly")
    void shouldFilterConfigurationsByTopicCorrectly() throws Exception {
        // Create configurations for different topics
        String[] topics = {"cpu-metrics", "memory-metrics", "disk-metrics"};

        for (int i = 0; i < topics.length; i++) {
            NotifierConfigurationRequest request = validRequest.toBuilder()
                    .notifier("TestNotifier" + i)
                    .topic(topics[i])
                    .description("Test configuration for " + topics[i])
                    .build();

            mockMvc.perform(post("/api/v1/configurations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Test filtering by each topic
        for (String topic : topics) {
            mockMvc.perform(get("/api/v1/configurations/topic/{topic}", topic))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].topic").value(topic))
                    .andExpect(jsonPath("$[0].description").value("Test configuration for " + topic));
        }

        // Test filtering by non-existent topic
        mockMvc.perform(get("/api/v1/configurations/topic/{topic}", "non-existent-topic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should handle enabled/disabled filtering correctly")
    void shouldHandleEnabledDisabledFilteringCorrectly() throws Exception {
        // Create enabled configuration
        mockMvc.perform(post("/api/v1/configurations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        // Create disabled configuration
        NotifierConfigurationRequest disabledRequest = validRequest.toBuilder()
                .notifier("DisabledNotifier")
                .topic("disabled-topic")
                .enabled(false)
                .description("Disabled configuration")
                .build();

        mockMvc.perform(post("/api/v1/configurations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(disabledRequest)))
                .andExpect(status().isCreated());

        // Test enabled configurations endpoint
        mockMvc.perform(get("/api/v1/configurations/enabled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].enabled").value(true))
                .andExpect(jsonPath("$[0].notifier").value("IntegrationTestNotifier"));

        // Test all configurations endpoint (should return both)
        mockMvc.perform(get("/api/v1/configurations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("Should maintain data consistency across operations")
    void shouldMaintainDataConsistencyAcrossOperations() throws Exception {
        // Create configuration
        String createResponse = mockMvc.perform(post("/api/v1/configurations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        NotifierConfigurationResponse created = objectMapper.readValue(createResponse, NotifierConfigurationResponse.class);
        String configId = created.getId();

        // Verify database state
        assertEquals(1, repository.count());
        assertTrue(repository.existsById(configId));
        assertTrue(repository.existsByNotifierAndTopic("IntegrationTestNotifier", "integration-test-topic"));

        // Update configuration
        validRequest.setDescription("Updated description");
        mockMvc.perform(put("/api/v1/configurations/{id}", configId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        // Verify database state after update
        assertEquals(1, repository.count());
        var updated = repository.findById(configId);
        assertTrue(updated.isPresent());
        assertEquals("Updated description", updated.get().getDescription());

        // Toggle enabled status
        mockMvc.perform(patch("/api/v1/configurations/{id}/toggle", configId))
                .andExpect(status().isOk());

        // Verify database state after toggle
        var toggled = repository.findById(configId);
        assertTrue(toggled.isPresent());
        assertFalse(toggled.get().isEnabled());

        // Delete configuration
        mockMvc.perform(delete("/api/v1/configurations/{id}", configId))
                .andExpect(status().isNoContent());

        // Verify database state after delete
        assertEquals(0, repository.count());
        assertFalse(repository.existsById(configId));
    }
}
*/
