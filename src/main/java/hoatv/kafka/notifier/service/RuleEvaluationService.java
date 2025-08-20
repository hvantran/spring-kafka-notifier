package hoatv.kafka.notifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RuleEvaluationService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleEvaluationService.class);
    
    private final ObjectMapper objectMapper;
    
    public boolean evaluateRules(Map<String, Object> rules, String message) {
        try {
            LOGGER.debug("Evaluating rules against message: {}", message);
            JsonNode messageNode = objectMapper.readTree(message);
            return evaluateNode(rules, messageNode);
            
        } catch (Exception e) {
            LOGGER.error("Error evaluating rules: {}", e.getMessage(), e);
            return false;
        }
    }
    
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
    
    @SuppressWarnings("unchecked")
    private boolean evaluateAndOperator(List<Map<String, Object>> conditions, JsonNode messageNode) {
        for (Map<String, Object> condition : conditions) {
            if (!evaluateNode(condition, messageNode)) {
                return false;
            }
        }
        return true;
    }
    
    @SuppressWarnings("unchecked")
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
            JsonNode fieldNode = getFieldValue(messageNode, field);
            if (fieldNode == null) return false;
            
            return compareValues(fieldNode, expectedValue) == 0;
        }
        
        // Direct value comparison
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
            JsonNode fieldNode = getFieldValue(messageNode, field);
            if (fieldNode == null) return false;
            
            for (Object value : values) {
                if (compareValues(fieldNode, value) == 0) {
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
        }
        
        return false;
    }
    
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
        
        // Direct value comparison against entire message
        Object directValue = condition.get("$value");
        if (directValue != null && messageNode.isNumber()) {
            double messageValue = messageNode.asDouble();
            double conditionValue = Double.parseDouble(directValue.toString());
            
            return operator.compare(messageValue, conditionValue);
        }
        
        return false;
    }
    
    private JsonNode getFieldValue(JsonNode messageNode, String field) {
        if (field.contains(".")) {
            // Handle nested field access
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
