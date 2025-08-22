package com.hoatv.kafka.notifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test cases for RuleEvaluationService class
 * Tests cover simple values, JSON objects, and complex logic operators
 */
@DisplayName("Rule Evaluation Service Tests")
class RuleEvaluationServiceTest {

    private RuleEvaluationService ruleEvaluationService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        ruleEvaluationService = new RuleEvaluationService(objectMapper);
    }

    // ========== SIMPLE NUMERIC VALUE TESTS ==========

    @Test
    @DisplayName("Should evaluate simple numeric value greater than 8")
    void shouldEvaluateSimpleNumericValueGreaterThan() {
        // Given
        String message = "10";
        Map<String, Object> rules = Map.of(
            "$gt", Map.of("$value", 8)
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertTrue(result, "10 should be greater than 8");
    }

    @Test
    @DisplayName("Should evaluate simple numeric value equals 8")
    void shouldEvaluateSimpleNumericValueEquals() {
        // Given
        String message = "8";
        Map<String, Object> rules = Map.of(
            "$eq", Map.of("$value", 8)
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertTrue(result, "8 should equal 8");
    }

    @Test
    @DisplayName("Should evaluate simple numeric value less than 8")
    void shouldEvaluateSimpleNumericValueLessThan() {
        // Given
        String message = "5";
        Map<String, Object> rules = Map.of(
            "$lt", Map.of("$value", 8)
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertTrue(result, "5 should be less than 8");
    }

    @Test
    @DisplayName("Should return false when simple numeric condition is not met")
    void shouldReturnFalseWhenSimpleNumericConditionNotMet() {
        // Given
        String message = "5";
        Map<String, Object> rules = Map.of(
            "$gt", Map.of("$value", 8)
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertFalse(result, "5 should not be greater than 8");
    }

    @Test
    @DisplayName("Should handle decimal values correctly")
    void shouldHandleDecimalValues() {
        // Given
        String message = "8.5";
        Map<String, Object> rules = Map.of(
            "$gt", Map.of("$value", 8.0)
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertTrue(result, "8.5 should be greater than 8.0");
    }

    @Test
    @DisplayName("Should handle negative numbers")
    void shouldHandleNegativeNumbers() {
        // Given
        String message = "-5";
        Map<String, Object> rules = Map.of(
            "$gt", Map.of("$value", -10)
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertTrue(result, "-5 should be greater than -10");
    }

    // ========== STRING VALUE TESTS ==========

    @Test
    @DisplayName("Should evaluate simple string value equals")
    void shouldEvaluateSimpleStringValueEquals() {
        // Given
        String message = "error";
        Map<String, Object> rules = Map.of(
            "$eq", Map.of("$value", "error")
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertTrue(result, "String 'error' should equal 'error'");
    }

    @Test
    @DisplayName("Should evaluate simple string value contains")
    void shouldEvaluateSimpleStringValueContains() {
        // Given
        String message = "CPU usage is high";
        Map<String, Object> rules = Map.of(
            "$contains", Map.of("$value", "usage")
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertTrue(result, "String should contain 'usage'");
    }

    @Test
    @DisplayName("Should evaluate simple string value in list")
    void shouldEvaluateSimpleStringValueIn() {
        // Given
        String message = "warning";
        Map<String, Object> rules = Map.of(
            "$in", Map.of("$values", List.of("error", "warning", "critical"))
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertTrue(result, "String 'warning' should be in the list");
    }

    // ========== BOOLEAN VALUE TESTS ==========

    @Test
    @DisplayName("Should evaluate boolean value true")
    void shouldEvaluateBooleanValueTrue() {
        // Given
        String message = "true";
        Map<String, Object> rules = Map.of(
            "$eq", Map.of("$value", true)
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertTrue(result, "Boolean 'true' should equal true");
    }

    @Test
    @DisplayName("Should evaluate boolean value false")
    void shouldEvaluateBooleanValueFalse() {
        // Given
        String message = "false";
        Map<String, Object> rules = Map.of(
            "$eq", Map.of("$value", false)
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertTrue(result, "Boolean 'false' should equal false");
    }

    // ========== JSON OBJECT FIELD TESTS ==========

    @Test
    @DisplayName("Should evaluate JSON object field greater than")
    void shouldEvaluateJsonObjectFieldGreaterThan() {
        // Given
        String message = "{\"cpu\": 85, \"memory\": 70}";
        Map<String, Object> rules = Map.of(
            "$gt", Map.of("$field", "cpu", "$value", 80)
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertTrue(result, "CPU 85 should be greater than 80");
    }

    @Test
    @DisplayName("Should evaluate JSON object field equals")
    void shouldEvaluateJsonObjectFieldEquals() {
        // Given
        String message = "{\"status\": \"error\", \"count\": 5}";
        Map<String, Object> rules = Map.of(
            "$eq", Map.of("$field", "status", "$value", "error")
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertTrue(result, "Status should equal 'error'");
    }

    @Test
    @DisplayName("Should evaluate nested JSON object field")
    void shouldEvaluateNestedJsonObjectField() {
        // Given
        String message = "{\"system\": {\"cpu\": {\"usage\": 90}}}";
        Map<String, Object> rules = Map.of(
            "$gt", Map.of("$field", "system.cpu.usage", "$value", 85)
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertTrue(result, "Nested CPU usage 90 should be greater than 85");
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
        assertFalse(result, "Missing field should return false");
    }

    // ========== COMPLEX LOGIC TESTS ==========

    @Test
    @DisplayName("Should evaluate AND operator with simple value")
    void shouldEvaluateAndOperatorWithSimpleValue() {
        // Given
        String message = "15";
        Map<String, Object> rules = Map.of(
            "$and", List.of(
                Map.of("$gt", Map.of("$value", 10)),
                Map.of("$lt", Map.of("$value", 20))
            )
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertTrue(result, "15 should be > 10 AND < 20");
    }

    @Test
    @DisplayName("Should evaluate OR operator with simple value")
    void shouldEvaluateOrOperatorWithSimpleValue() {
        // Given
        String message = "25";
        Map<String, Object> rules = Map.of(
            "$or", List.of(
                Map.of("$lt", Map.of("$value", 10)),
                Map.of("$gt", Map.of("$value", 20))
            )
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertTrue(result, "25 should satisfy (< 10 OR > 20)");
    }

    @Test
    @DisplayName("Should evaluate AND operator with JSON object")
    void shouldEvaluateAndOperatorWithJsonObject() {
        // Given
        String message = "{\"cpu\": 85, \"memory\": 75}";
        Map<String, Object> rules = Map.of(
            "$and", List.of(
                Map.of("$gt", Map.of("$field", "cpu", "$value", 80)),
                Map.of("$gt", Map.of("$field", "memory", "$value", 70))
            )
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertTrue(result, "Both CPU > 80 AND memory > 70 should be true");
    }

    // ========== EDGE CASES AND ERROR HANDLING ==========

    @Test
    @DisplayName("Should handle null message")
    void shouldHandleNullMessage() {
        // Given
        String message = null;
        Map<String, Object> rules = Map.of(
            "$gt", Map.of("$value", 8)
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertFalse(result, "Null message should return false");
    }

    @Test
    @DisplayName("Should handle empty message")
    void shouldHandleEmptyMessage() {
        // Given
        String message = "";
        Map<String, Object> rules = Map.of(
            "$gt", Map.of("$value", 8)
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertFalse(result, "Empty message should return false");
    }

    @Test
    @DisplayName("Should handle invalid JSON gracefully")
    void shouldHandleInvalidJsonGracefully() {
        // Given
        String message = "{invalid json}";
        Map<String, Object> rules = Map.of(
            "$contains", Map.of("$value", "invalid")
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertTrue(result, "Should treat invalid JSON as string and evaluate contains");
    }

    @Test
    @DisplayName("Should handle unsupported operator")
    void shouldHandleUnsupportedOperator() {
        // Given
        String message = "10";
        Map<String, Object> rules = Map.of(
            "$unsupported", Map.of("$value", 8)
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertFalse(result, "Unsupported operator should return false");
    }

    // ========== REAL-WORLD USE CASE TESTS ==========

    @Test
    @DisplayName("Should monitor CPU usage threshold - your original requirement")
    void shouldMonitorCpuUsageThreshold() {
        // Given - CPU usage monitoring scenario
        String message = "{\"timestamp\": \"2025-08-23T10:30:00Z\", \"metrics\": {\"cpu\": 92, \"memory\": 75}, \"host\": \"server1\"}";
        Map<String, Object> rules = Map.of(
            "$gt", Map.of("$field", "metrics.cpu", "$value", 90)
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertTrue(result, "Should trigger alert when CPU > 90%");
    }

    @Test
    @DisplayName("Should handle simple threshold monitoring - value > 8")
    void shouldHandleSimpleThresholdMonitoring() {
        // Given - Simple value monitoring (like your original requirement)
        String message = "12";
        Map<String, Object> rules = Map.of(
            "$gt", Map.of("$value", 8)
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertTrue(result, "Should trigger when value > 8");
    }

    @Test
    @DisplayName("Should not trigger when value <= 8")
    void shouldNotTriggerWhenValueLessOrEqualTo8() {
        // Given
        String message = "7";
        Map<String, Object> rules = Map.of(
            "$gt", Map.of("$value", 8)
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertFalse(result, "Should not trigger when value <= 8");
    }

    @Test
    @DisplayName("Should handle exactly 8")
    void shouldHandleExactly8() {
        // Given
        String message = "8";
        Map<String, Object> rules = Map.of(
            "$gt", Map.of("$value", 8)
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertFalse(result, "8 should not be greater than 8");
    }

    @Test
    @DisplayName("Should handle boundary case - 8.1")
    void shouldHandleBoundaryCase() {
        // Given
        String message = "8.1";
        Map<String, Object> rules = Map.of(
            "$gt", Map.of("$value", 8)
        );

        // When
        boolean result = ruleEvaluationService.evaluateRules(rules, message);

        // Then
        assertTrue(result, "8.1 should be greater than 8");
    }
}
