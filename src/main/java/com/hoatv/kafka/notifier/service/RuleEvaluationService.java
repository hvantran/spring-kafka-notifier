package com.hoatv.kafka.notifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Enhanced Rule Evaluation Service that handles both simple values and JSON objects
 * <p>
 * This service can evaluate rules against:
 * 1. Simple values: "8", "hello", "true", "42.5"
 * 2. JSON objects: {"cpu": 85, "status": "error"}
 * 3. JSON arrays: [1, 2, 3]
 * <p>
 * For simple values, rules can use direct value comparison without field references.
 */
@Service("enhancedRuleEvaluationService")
@RequiredArgsConstructor
public class RuleEvaluationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleEvaluationService.class);

    private final ObjectMapper objectMapper;

    /**
     * Evaluate rules against a Kafka message
     * Handles both simple values and complex JSON objects
     */
    public boolean evaluateRules(Map<String, Object> rules, String message) {
        try {
            LOGGER.debug("Evaluating rules against message: {}", message);

            JsonNode messageNode = parseMessage(message);
            return evaluateNode(rules, messageNode);

        } catch (Exception e) {
            LOGGER.error("Error evaluating rules: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Parse message into JsonNode, handling both simple values and JSON objects
     */
    private JsonNode parseMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return NullNode.getInstance();
        }

        String trimmedMessage = message.trim();

        try {
            return objectMapper.readTree(trimmedMessage);
        } catch (Exception e) {
            LOGGER.debug("Message is not valid JSON, treating as simple value: {}", trimmedMessage);
            return parseAsSimpleValue(trimmedMessage);
        }
    }

    /**
     * Parse simple values into appropriate JsonNode types
     */
    private JsonNode parseAsSimpleValue(String value) {
        try {
            if (value.contains(".")) {
                double doubleValue = Double.parseDouble(value);
                return DoubleNode.valueOf(doubleValue);
            } else {
                long longValue = Long.parseLong(value);
                if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                    return IntNode.valueOf((int) longValue);
                } else {
                    return LongNode.valueOf(longValue);
                }
            }
        } catch (NumberFormatException e) {
            // Not a number, continue with other types
        }

        // Try to parse as boolean
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return BooleanNode.valueOf(Boolean.parseBoolean(value));
        }

        // Default to string
        return TextNode.valueOf(value);
    }

    /**
     * Evaluate a rule node against the message
     */
    @SuppressWarnings("unchecked")
    private boolean evaluateNode(Map<String, Object> rules, JsonNode messageNode) {
        for (Map.Entry<String, Object> entry : rules.entrySet()) {
            String operator = entry.getKey();
            Object value = entry.getValue();

            switch (operator) {
                case "$and":
                    if (!evaluateAndOperator((List<Map<String, Object>>) value, messageNode)) {
                        return false;
                    }
                    break;

                case "$or":
                    if (!evaluateOrOperator((List<Map<String, Object>>) value, messageNode)) {
                        return false;
                    }
                    break;

                case "$gt":
                    if (!evaluateGreaterThan((Map<String, Object>) value, messageNode)) {
                        return false;
                    }
                    break;

                case "$gte":
                    if (!evaluateGreaterThanOrEqual((Map<String, Object>) value, messageNode)) {
                        return false;
                    }
                    break;

                case "$lt":
                    if (!evaluateLessThan((Map<String, Object>) value, messageNode)) {
                        return false;
                    }
                    break;

                case "$lte":
                    if (!evaluateLessThanOrEqual((Map<String, Object>) value, messageNode)) {
                        return false;
                    }
                    break;

                case "$eq":
                    if (!evaluateEquals((Map<String, Object>) value, messageNode)) {
                        return false;
                    }
                    break;

                case "$ne":
                    if (!evaluateNotEquals((Map<String, Object>) value, messageNode)) {
                        return false;
                    }
                    break;

                case "$in":
                    if (!evaluateIn((Map<String, Object>) value, messageNode)) {
                        return false;
                    }
                    break;

                case "$contains":
                    if (!evaluateContains((Map<String, Object>) value, messageNode)) {
                        return false;
                    }
                    break;

                default:
                    LOGGER.warn("Unsupported operator: {}", operator);
                    return false;
            }
        }

        return true;
    }

    private boolean evaluateAndOperator(List<Map<String, Object>> conditions, JsonNode messageNode) {
        for (Map<String, Object> condition : conditions) {
            if (!evaluateNode(condition, messageNode)) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateOrOperator(List<Map<String, Object>> conditions, JsonNode messageNode) {
        for (Map<String, Object> condition : conditions) {
            if (evaluateNode(condition, messageNode)) {
                return true;
            }
        }
        return false;
    }

    private boolean evaluateGreaterThan(Map<String, Object> condition, JsonNode messageNode) {
        return evaluateComparison(condition, messageNode, (messageValue, conditionValue) ->
                messageValue > conditionValue);
    }

    private boolean evaluateGreaterThanOrEqual(Map<String, Object> condition, JsonNode messageNode) {
        return evaluateComparison(condition, messageNode, (messageValue, conditionValue) ->
                messageValue >= conditionValue);
    }

    private boolean evaluateLessThan(Map<String, Object> condition, JsonNode messageNode) {
        return evaluateComparison(condition, messageNode, (messageValue, conditionValue) ->
                messageValue < conditionValue);
    }

    private boolean evaluateLessThanOrEqual(Map<String, Object> condition, JsonNode messageNode) {
        return evaluateComparison(condition, messageNode, (messageValue, conditionValue) ->
                messageValue <= conditionValue);
    }

    private boolean evaluateEquals(Map<String, Object> condition, JsonNode messageNode) {
        String field = (String) condition.get("$field");
        Object expectedValue = condition.get("$value");

        if (field != null) {
            // Field-based comparison for JSON objects
            JsonNode fieldNode = getFieldValue(messageNode, field);
            if (fieldNode == null) return false;

            return compareValues(fieldNode, expectedValue) == 0;
        }

        // Direct value comparison for simple values
        Object directValue = condition.get("$value");
        if (directValue != null) {
            return compareValues(messageNode, directValue) == 0;
        }

        return false;
    }

    private boolean evaluateNotEquals(Map<String, Object> condition, JsonNode messageNode) {
        return !evaluateEquals(condition, messageNode);
    }

    @SuppressWarnings("unchecked")
    private boolean evaluateIn(Map<String, Object> condition, JsonNode messageNode) {
        String field = (String) condition.get("$field");
        List<Object> values = (List<Object>) condition.get("$values");

        if (field != null && values != null) {
            // Field-based comparison
            JsonNode fieldNode = getFieldValue(messageNode, field);
            if (fieldNode == null) return false;

            for (Object value : values) {
                if (compareValues(fieldNode, value) == 0) {
                    return true;
                }
            }
        } else if (values != null) {
            // Direct value comparison for simple values
            for (Object value : values) {
                if (compareValues(messageNode, value) == 0) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean evaluateContains(Map<String, Object> condition, JsonNode messageNode) {
        String field = (String) condition.get("$field");
        String searchValue = (String) condition.get("$value");

        if (field != null && searchValue != null) {
            JsonNode fieldNode = getFieldValue(messageNode, field);
            if (fieldNode == null || !fieldNode.isTextual()) return false;
            return fieldNode.asText().toLowerCase().contains(searchValue.toLowerCase());
        } else if (searchValue != null) {
            if (messageNode.isTextual()) {
                return messageNode.asText().toLowerCase().contains(searchValue.toLowerCase());
            }
        }

        return false;
    }

    /**
     * Enhanced comparison method that handles numeric comparisons
     */
    private boolean evaluateComparison(Map<String, Object> condition, JsonNode messageNode,
                                       ComparisonOperator operator) {
        String field = (String) condition.get("$field");
        Object compareValue = condition.get("$value");

        if (field != null && compareValue != null) {
            JsonNode fieldNode = getFieldValue(messageNode, field);
            if (fieldNode == null || !fieldNode.isNumber()) return false;
            double messageValue = fieldNode.asDouble();
            double conditionValue = Double.parseDouble(compareValue.toString());
            return operator.compare(messageValue, conditionValue);
        }

        // Direct value comparison for simple values
        Object directValue = condition.get("$value");
        if (directValue != null && messageNode.isNumber()) {
            double messageValue = messageNode.asDouble();
            double conditionValue = Double.parseDouble(directValue.toString());
            return operator.compare(messageValue, conditionValue);
        }

        return false;
    }

    /**
     * Get field value from JSON object, supporting nested fields
     */
    private JsonNode getFieldValue(JsonNode messageNode, String field) {
        if (field.contains(".")) {
            // Handle nested field access like "system.cpu.usage"
            String[] parts = field.split("\\.");
            JsonNode current = messageNode;

            for (String part : parts) {
                if (current == null || !current.has(part)) {
                    return null;
                }
                current = current.get(part);
            }

            return current;
        } else {
            return messageNode.get(field);
        }
    }

    /**
     * Compare JsonNode with expected value
     */
    private int compareValues(JsonNode messageNode, Object expectedValue) {
        if (messageNode.isNumber() && expectedValue instanceof Number) {
            double messageValue = messageNode.asDouble();
            double expectedDouble = ((Number) expectedValue).doubleValue();
            return Double.compare(messageValue, expectedDouble);
        } else if (messageNode.isTextual()) {
            String messageText = messageNode.asText();
            String expectedText = expectedValue.toString();
            return messageText.compareTo(expectedText);
        } else if (messageNode.isBoolean()) {
            boolean messageBoolean = messageNode.asBoolean();
            boolean expectedBoolean = Boolean.parseBoolean(expectedValue.toString());
            return Boolean.compare(messageBoolean, expectedBoolean);
        }

        return messageNode.toString().compareTo(expectedValue.toString());
    }

    @FunctionalInterface
    private interface ComparisonOperator {
        boolean compare(double messageValue, double conditionValue);
    }
}
