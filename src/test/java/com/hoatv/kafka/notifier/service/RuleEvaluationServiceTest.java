package com.hoatv.kafka.notifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoatv.kafka.notifier.config.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {RuleEvaluationService.class})
@ContextConfiguration(classes = TestConfig.class)
@DisplayName("RuleEvaluationService Tests")
class RuleEvaluationServiceTest {

    @Autowired
    private ObjectMapper objectMapper;
    
    private RuleEvaluationService ruleEvaluationService;
    
    @BeforeEach
    void setUp() {
        ruleEvaluationService = new RuleEvaluationService(objectMapper);
    }
    
    @Test
    @DisplayName("Should evaluate greater than rule correctly")
    void shouldEvaluateGreaterThanRule() {
        // Given
        String message = "{\"value\": 85, \"name\": \"cpu-usage\"}";
        Map<String, Object> rules = Map.of(
            "$gt", Map.of("$field", "value", "$value", 80)
        );
        
        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);
        
        // Then
        assertTrue(result, "Should return true when value 85 > 80");
    }
    
    @Test
    @DisplayName("Should evaluate greater than rule as false when condition not met")
    void shouldEvaluateGreaterThanRuleAsFalse() {
        // Given
        String message = "{\"value\": 75, \"name\": \"cpu-usage\"}";
        Map<String, Object> rules = Map.of(
            "$gt", Map.of("$field", "value", "$value", 80)
        );
        
        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);
        
        // Then
        assertFalse(result, "Should return false when value 75 is not > 80");
    }
    
    @Test
    @DisplayName("Should evaluate direct value greater than rule correctly")
    void shouldEvaluateDirectValueGreaterThan() {
        // Given
        String message = "85";
        Map<String, Object> rules = Map.of(
            "$gt", Map.of("$value", 80)
        );
        
        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);
        
        // Then
        assertTrue(result, "Should return true when direct value 85 > 80");
    }
    
    @Test
    @DisplayName("Should evaluate AND operator correctly - all conditions true")
    void shouldEvaluateAndOperatorAllTrue() {
        // Given
        String message = "{\"cpu\": 85, \"memory\": 70}";
        Map<String, Object> rules = Map.of(
            "$and", List.of(
                Map.of("$gt", Map.of("$field", "cpu", "$value", 80)),
                Map.of("$gt", Map.of("$field", "memory", "$value", 60))
            )
        );
        
        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);
        
        // Then
        assertTrue(result, "Should return true when all AND conditions are met");
    }
    
    @Test
    @DisplayName("Should evaluate AND operator correctly - one condition false")
    void shouldEvaluateAndOperatorOneFalse() {
        // Given
        String message = "{\"cpu\": 85, \"memory\": 50}";
        Map<String, Object> rules = Map.of(
            "$and", List.of(
                Map.of("$gt", Map.of("$field", "cpu", "$value", 80)),
                Map.of("$gt", Map.of("$field", "memory", "$value", 60))
            )
        );
        
        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);
        
        // Then
        assertFalse(result, "Should return false when one AND condition is not met");
    }
    
    @Test
    @DisplayName("Should evaluate OR operator correctly - one condition true")
    void shouldEvaluateOrOperatorOneTrue() {
        // Given
        String message = "{\"cpu\": 85, \"memory\": 50}";
        Map<String, Object> rules = Map.of(
            "$or", List.of(
                Map.of("$gt", Map.of("$field", "cpu", "$value", 80)),
                Map.of("$gt", Map.of("$field", "memory", "$value", 60))
            )
        );
        
        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);
        
        // Then
        assertTrue(result, "Should return true when at least one OR condition is met");
    }
    
    @Test
    @DisplayName("Should evaluate OR operator correctly - all conditions false")
    void shouldEvaluateOrOperatorAllFalse() {
        // Given
        String message = "{\"cpu\": 75, \"memory\": 50}";
        Map<String, Object> rules = Map.of(
            "$or", List.of(
                Map.of("$gt", Map.of("$field", "cpu", "$value", 80)),
                Map.of("$gt", Map.of("$field", "memory", "$value", 60))
            )
        );
        
        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);
        
        // Then
        assertFalse(result, "Should return false when all OR conditions are not met");
    }
    
    @Test
    @DisplayName("Should evaluate equals rule correctly")
    void shouldEvaluateEqualsRule() {
        // Given
        String message = "{\"status\": \"error\", \"code\": 500}";
        Map<String, Object> rules = Map.of(
            "$eq", Map.of("$field", "status", "$value", "error")
        );
        
        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);
        
        // Then
        assertTrue(result, "Should return true when string values match exactly");
    }
    
    @Test
    @DisplayName("Should evaluate not equals rule correctly")
    void shouldEvaluateNotEqualsRule() {
        // Given
        String message = "{\"status\": \"error\", \"code\": 500}";
        Map<String, Object> rules = Map.of(
            "$ne", Map.of("$field", "status", "$value", "success")
        );
        
        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);
        
        // Then
        assertTrue(result, "Should return true when string values do not match");
    }
    
    @Test
    @DisplayName("Should evaluate contains rule correctly")
    void shouldEvaluateContainsRule() {
        // Given
        String message = "{\"message\": \"CPU usage is high\", \"level\": \"warning\"}";
        Map<String, Object> rules = Map.of(
            "$contains", Map.of("$field", "message", "$value", "CPU usage")
        );
        
        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);
        
        // Then
        assertTrue(result, "Should return true when string contains the specified value");
    }
    
    @Test
    @DisplayName("Should evaluate contains rule as case insensitive")
    void shouldEvaluateContainsRuleCaseInsensitive() {
        // Given
        String message = "{\"message\": \"CPU Usage Is High\", \"level\": \"warning\"}";
        Map<String, Object> rules = Map.of(
            "$contains", Map.of("$field", "message", "$value", "cpu usage")
        );
        
        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);
        
        // Then
        assertTrue(result, "Should return true for case insensitive contains match");
    }
    
    @Test
    @DisplayName("Should evaluate in rule correctly")
    void shouldEvaluateInRule() {
        // Given
        String message = "{\"level\": \"error\", \"code\": 404}";
        Map<String, Object> rules = Map.of(
            "$in", Map.of("$field", "level", "$values", List.of("error", "critical", "fatal"))
        );
        
        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);
        
        // Then
        assertTrue(result, "Should return true when field value is in the specified list");
    }
    
    @Test
    @DisplayName("Should evaluate nested field access correctly")
    void shouldEvaluateNestedFieldAccess() {
        // Given
        String message = "{\"system\": {\"cpu\": {\"usage\": 85}}, \"timestamp\": \"2023-01-01\"}";
        Map<String, Object> rules = Map.of(
            "$gt", Map.of("$field", "system.cpu.usage", "$value", 80)
        );
        
        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);
        
        // Then
        assertTrue(result, "Should return true when nested field value meets condition");
    }
    
    @Test
    @DisplayName("Should handle missing field gracefully")
    void shouldHandleMissingFieldGracefully() {
        // Given
        String message = "{\"cpu\": 85}";
        Map<String, Object> rules = Map.of(
            "$gt", Map.of("$field", "memory", "$value", 80)
        );
        
        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);
        
        // Then
        assertFalse(result, "Should return false when field is missing");
    }
    
    @Test
    @DisplayName("Should handle invalid JSON gracefully")
    void shouldHandleInvalidJsonGracefully() {
        // Given
        String invalidMessage = "{invalid json";
        Map<String, Object> rules = Map.of(
            "$gt", Map.of("$field", "value", "$value", 80)
        );
        
        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, invalidMessage);
        
        // Then
        assertFalse(result, "Should return false when JSON is invalid");
    }
    
    @Test
    @DisplayName("Should handle unsupported operator gracefully")
    void shouldHandleUnsupportedOperatorGracefully() {
        // Given
        String message = "{\"value\": 85}";
        Map<String, Object> rules = Map.of(
            "$unsupported", Map.of("$field", "value", "$value", 80)
        );
        
        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);
        
        // Then
        assertFalse(result, "Should return false when operator is unsupported");
    }
    
    @Test
    @DisplayName("Should evaluate complex nested AND/OR rules")
    void shouldEvaluateComplexNestedRules() {
        // Given
        String message = "{\"cpu\": 85, \"memory\": 70, \"disk\": 60, \"status\": \"warning\"}";
        Map<String, Object> rules = Map.of(
            "$and", List.of(
                Map.of("$or", List.of(
                    Map.of("$gt", Map.of("$field", "cpu", "$value", 80)),
                    Map.of("$gt", Map.of("$field", "memory", "$value", 80))
                )),
                Map.of("$eq", Map.of("$field", "status", "$value", "warning"))
            )
        );
        
        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);
        
        // Then
        assertTrue(result, "Should handle complex nested AND/OR rules correctly");
    }
    
    @Test
    @DisplayName("Should evaluate all comparison operators correctly")
    void shouldEvaluateAllComparisonOperators() {
        // Given
        String message = "{\"value\": 75}";
        
        // Test $gte
        Map<String, Object> gteRules = Map.of("$gte", Map.of("$field", "value", "$value", 75));
        assertTrue(ruleEvaluationService.evaluateRules(gteRules, message), "GTE should work for equal values");
        
        // Test $lt
        Map<String, Object> ltRules = Map.of("$lt", Map.of("$field", "value", "$value", 80));
        assertTrue(ruleEvaluationService.evaluateRules(ltRules, message), "LT should work when value is less");
        
        // Test $lte
        Map<String, Object> lteRules = Map.of("$lte", Map.of("$field", "value", "$value", 75));
        assertTrue(ruleEvaluationService.evaluateRules(lteRules, message), "LTE should work for equal values");
    }
}
