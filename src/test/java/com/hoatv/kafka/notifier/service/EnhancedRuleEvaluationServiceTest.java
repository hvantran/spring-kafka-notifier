//package com.hoatv.kafka.notifier.service;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.DisplayName;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.util.List;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest
//@DisplayName("Enhanced Rule Evaluation Service - Simple Values Tests")
//class RuleEvaluationServiceTest {
//
//    private RuleEvaluationService ruleEvaluationService;
//    private ObjectMapper objectMapper;
//
//    @BeforeEach
//    void setUp() {
//        objectMapper = new ObjectMapper();
//        ruleEvaluationService = new RuleEvaluationService(objectMapper);
//    }
//
//    // ========== SIMPLE NUMERIC VALUE TESTS ==========
//
//    @Test
//    @DisplayName("Should evaluate simple numeric value - greater than")
//    void shouldEvaluateSimpleNumericValueGreaterThan() {
//        // Given
//        String message = "8";  // Simple numeric value
//        Map<String, Object> rules = Map.of(
//            "$gt", Map.of("$value", 5)
//        );
//
//        // When
//        boolean result = ruleEvaluationService.evaluateRules(rules, message);
//
//        // Then
//        assertTrue(result, "8 should be greater than 5");
//    }
//
//    @Test
//    @DisplayName("Should evaluate simple numeric value - less than")
//    void shouldEvaluateSimpleNumericValueLessThan() {
//        // Given
//        String message = "8";
//        Map<String, Object> rules = Map.of(
//            "$lt", Map.of("$value", 10)
//        );
//
//        // When
//        boolean result = ruleEvaluationService.evaluateRules(rules, message);
//
//        // Then
//        assertTrue(result, "8 should be less than 10");
//    }
//
//    @Test
//    @DisplayName("Should evaluate simple numeric value - equals")
//    void shouldEvaluateSimpleNumericValueEquals() {
//        // Given
//        String message = "42";
//        Map<String, Object> rules = Map.of(
//            "$eq", Map.of("$value", 42)
//        );
//
//        // When
//        boolean result = ruleEvaluationService.evaluateRules(rules, message);
//
//        // Then
//        assertTrue(result, "42 should equal 42");
//    }
//
//    @Test
//    @DisplayName("Should evaluate decimal values correctly")
//    void shouldEvaluateDecimalValues() {
//        // Given
//        String message = "8.5";
//        Map<String, Object> rules = Map.of(
//            "$gt", Map.of("$value", 8.0)
//        );
//
//        // When
//        boolean result = ruleEvaluationService.evaluateRules(rules, message);
//
//        // Then
//        assertTrue(result, "8.5 should be greater than 8.0");
//    }
//
//    // ========== SIMPLE STRING VALUE TESTS ==========
//
//    @Test
//    @DisplayName("Should evaluate simple string value - equals")
//    void shouldEvaluateSimpleStringValueEquals() {
//        // Given
//        String message = "error";
//        Map<String, Object> rules = Map.of(
//            "$eq", Map.of("$value", "error")
//        );
//
//        // When
//        boolean result = ruleEvaluationService.evaluateRules(rules, message);
//
//        // Then
//        assertTrue(result, "String 'error' should equal 'error'");
//    }
//
//    @Test
//    @DisplayName("Should evaluate simple string value - contains")
//    void shouldEvaluateSimpleStringValueContains() {
//        // Given
//        String message = "CPU usage is high";
//        Map<String, Object> rules = Map.of(
//            "$contains", Map.of("$value", "usage")
//        );
//
//        // When
//        boolean result = ruleEvaluationService.evaluateRules(rules, message);
//
//        // Then
//        assertTrue(result, "String should contain 'usage'");
//    }
//
//    @Test
//    @DisplayName("Should evaluate simple string value - in list")
//    void shouldEvaluateSimpleStringValueIn() {
//        // Given
//        String message = "warning";
//        Map<String, Object> rules = Map.of(
//            "$in", Map.of("$values", List.of("error", "warning", "critical"))
//        );
//
//        // When
//        boolean result = ruleEvaluationService.evaluateRules(rules, message);
//
//        // Then
//        assertTrue(result, "String 'warning' should be in the list");
//    }
//
//    // ========== BOOLEAN VALUE TESTS ==========
//
//    @Test
//    @DisplayName("Should evaluate boolean value - true")
//    void shouldEvaluateBooleanValueTrue() {
//        // Given
//        String message = "true";
//        Map<String, Object> rules = Map.of(
//            "$eq", Map.of("$value", true)
//        );
//
//        // When
//        boolean result = ruleEvaluationService.evaluateRules(rules, message);
//
//        // Then
//        assertTrue(result, "Boolean 'true' should equal true");
//    }
//
//    @Test
//    @DisplayName("Should evaluate boolean value - false")
//    void shouldEvaluateBooleanValueFalse() {
//        // Given
//        String message = "false";
//        Map<String, Object> rules = Map.of(
//            "$eq", Map.of("$value", false)
//        );
//
//        // When
//        boolean result = ruleEvaluationService.evaluateRules(rules, message);
//
//        // Then
//        assertTrue(result, "Boolean 'false' should equal false");
//    }
//
//    // ========== COMPLEX RULES WITH SIMPLE VALUES ==========
//
//    @Test
//    @DisplayName("Should evaluate AND operator with simple numeric value")
//    void shouldEvaluateAndOperatorWithSimpleValue() {
//        // Given
//        String message = "15";
//        Map<String, Object> rules = Map.of(
//            "$and", List.of(
//                Map.of("$gt", Map.of("$value", 10)),
//                Map.of("$lt", Map.of("$value", 20))
//            )
//        );
//
//        // When
//        boolean result = ruleEvaluationService.evaluateRules(rules, message);
//
//        // Then
//        assertTrue(result, "15 should be > 10 AND < 20");
//    }
//
//    @Test
//    @DisplayName("Should evaluate OR operator with simple numeric value")
//    void shouldEvaluateOrOperatorWithSimpleValue() {
//        // Given
//        String message = "25";
//        Map<String, Object> rules = Map.of(
//            "$or", List.of(
//                Map.of("$lt", Map.of("$value", 10)),  // false: 25 < 10
//                Map.of("$gt", Map.of("$value", 20))   // true: 25 > 20
//            )
//        );
//
//        // When
//        boolean result = ruleEvaluationService.evaluateRules(rules, message);
//
//        // Then
//        assertTrue(result, "25 should satisfy: < 10 OR > 20");
//    }
//
//    // ========== COMPARISON WITH JSON OBJECTS ==========
//
//    @Test
//    @DisplayName("Should still work with JSON objects - field-based rules")
//    void shouldWorkWithJsonObjectsFieldBased() {
//        // Given
//        String message = "{\"cpu\": 85, \"status\": \"warning\"}";
//        Map<String, Object> rules = Map.of(
//            "$gt", Map.of("$field", "cpu", "$value", 80)
//        );
//
//        // When
//        boolean result = ruleEvaluationService.evaluateRules(rules, message);
//
//        // Then
//        assertTrue(result, "Should work with field-based rules on JSON objects");
//    }
//
//    @Test
//    @DisplayName("Should handle mixed AND conditions - simple value and JSON field")
//    void shouldHandleMixedConditions() {
//        // This would be used if you want to evaluate both the entire message value
//        // and specific fields, but typically you'd use one approach per message type
//
//        String message = "{\"value\": 8, \"status\": \"active\"}";
//        Map<String, Object> rules = Map.of(
//            "$and", List.of(
//                Map.of("$gt", Map.of("$field", "value", "$value", 5)),
//                Map.of("$eq", Map.of("$field", "status", "$value", "active"))
//            )
//        );
//
//        // When
//        boolean result = ruleEvaluationService.evaluateRules(rules, message);
//
//        // Then
//        assertTrue(result, "Should handle mixed field-based conditions");
//    }
//
//    // ========== EDGE CASES ==========
//
//    @Test
//    @DisplayName("Should handle empty string gracefully")
//    void shouldHandleEmptyString() {
//        // Given
//        String message = "";
//        Map<String, Object> rules = Map.of(
//            "$eq", Map.of("$value", "")
//        );
//
//        // When
//        boolean result = ruleEvaluationService.evaluateRules(rules, message);
//
//        // Then
//        assertTrue(result, "Empty string should equal empty string");
//    }
//
//    @Test
//    @DisplayName("Should handle null message gracefully")
//    void shouldHandleNullMessage() {
//        // Given
//        String message = null;
//        Map<String, Object> rules = Map.of(
//            "$eq", Map.of("$value", "anything")
//        );
//
//        // When
//        boolean result = ruleEvaluationService.evaluateRules(rules, message);
//
//        // Then
//        assertFalse(result, "Null message should not match any rule");
//    }
//
//    @Test
//    @DisplayName("Should handle invalid number format gracefully")
//    void shouldHandleInvalidNumberFormat() {
//        // Given
//        String message = "not_a_number";
//        Map<String, Object> rules = Map.of(
//            "$gt", Map.of("$value", 5)
//        );
//
//        // When
//        boolean result = ruleEvaluationService.evaluateRules(rules, message);
//
//        // Then
//        assertFalse(result, "Non-numeric string should not satisfy numeric comparison");
//    }
//
//    // ========== REALISTIC USE CASES ==========
//
//    @Test
//    @DisplayName("CPU Usage Alert - Simple Value")
//    void cpuUsageAlertSimpleValue() {
//        // Given: Kafka message contains just CPU percentage
//        String message = "87";
//
//        // Rule: Alert if CPU > 85%
//        Map<String, Object> rules = Map.of(
//            "$gt", Map.of("$value", 85)
//        );
//
//        // When
//        boolean result = ruleEvaluationService.evaluateRules(rules, message);
//
//        // Then
//        assertTrue(result, "Should trigger alert for CPU usage 87% > 85%");
//    }
//
//    @Test
//    @DisplayName("Temperature Monitoring - Range Check")
//    void temperatureMonitoringRangeCheck() {
//        // Given: Temperature sensor sends just the temperature value
//        String message = "45.5";
//
//        // Rule: Alert if temperature is outside normal range (20-40°C)
//        Map<String, Object> rules = Map.of(
//            "$or", List.of(
//                Map.of("$lt", Map.of("$value", 20)),  // Too cold
//                Map.of("$gt", Map.of("$value", 40))   // Too hot
//            )
//        );
//
//        // When
//        boolean result = ruleEvaluationService.evaluateRules(rules, message);
//
//        // Then
//        assertTrue(result, "Should trigger alert for temperature 45.5°C > 40°C");
//    }
//
//    @Test
//    @DisplayName("System Status Monitoring - String Matching")
//    void systemStatusMonitoringStringMatching() {
//        // Given: System sends status updates as simple strings
//        String message = "ERROR";
//
//        // Rule: Alert on error conditions
//        Map<String, Object> rules = Map.of(
//            "$in", Map.of("$values", List.of("ERROR", "CRITICAL", "FATAL"))
//        );
//
//        // When
//        boolean result = ruleEvaluationService.evaluateRules(rules, message);
//
//        // Then
//        assertTrue(result, "Should trigger alert for ERROR status");
//    }
//
//    @Test
//    @DisplayName("Log Level Filtering - Contains Check")
//    void logLevelFilteringContainsCheck() {
//        // Given: Log message as simple string
//        String message = "Database connection failed with timeout error";
//
//        // Rule: Alert on messages containing database issues
//        Map<String, Object> rules = Map.of(
//            "$and", List.of(
//                Map.of("$contains", Map.of("$value", "database")),
//                Map.of("$contains", Map.of("$value", "failed"))
//            )
//        );
//
//        // When
//        boolean result = ruleEvaluationService.evaluateRules(rules, message);
//
//        // Then
//        assertTrue(result, "Should trigger alert for database failure messages");
//    }
//}
