package exception;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import engine.ValidationResult;

public class ValidationException extends Exception {
    
    private final Map<String, List<String>> fieldErrors;
    private final String globalMessage;
    
    // Constructor for field-level validation errors
    public ValidationException(ValidationResult validationResult) {
        super("Validation failed");
        this.fieldErrors = validationResult.getErrors();
        this.globalMessage = "Validation failed for multiple fields";
    }
    
    // Constructor for single error message
    public ValidationException(String message) {
        super(message);
        this.fieldErrors = new HashMap<>();
        this.globalMessage = message;
    }
    
    // Constructor for both field errors and custom message
    public ValidationException(String message, ValidationResult validationResult) {
        super(message);
        this.fieldErrors = validationResult.getErrors();
        this.globalMessage = message;
    }
    
    // Get all field-level validation errors
    public Map<String, List<String>> getFieldErrors() {
        return fieldErrors;
    }
    
    // Get errors for a specific field
    public List<String> getFieldErrors(String fieldName) {
        return fieldErrors.getOrDefault(fieldName, null);
    }
    
    // Check if a specific field has errors
    public boolean hasFieldErrors(String fieldName) {
        return fieldErrors.containsKey(fieldName) && !fieldErrors.get(fieldName).isEmpty();
    }
    
    // Get total number of validation errors
    public int getErrorCount() {
        return fieldErrors.values().stream()
                .mapToInt(List::size)
                .sum();
    }
    
    // Get global error message
    public String getGlobalMessage() {
        return globalMessage;
    }
    
    // Format error messages for display or logging
    public String getFormattedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(globalMessage).append("\n");
        
        if (!fieldErrors.isEmpty()) {
            sb.append("Field errors:\n");
            fieldErrors.forEach((field, errors) -> {
                sb.append(field).append(":\n");
                errors.forEach(error -> sb.append("  - ").append(error).append("\n"));
            });
        }
        
        return sb.toString();
    }
    
    // Helper method to check if there are any errors
    public boolean hasErrors() {
        return !fieldErrors.isEmpty();
    }
    
    @Override
    public String getMessage() {
        return getFormattedMessage();
    }
    
    // Convert to JSON format (useful for REST APIs)
    public String toJSON() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"message\":\"").append(globalMessage).append("\",");
        json.append("\"errors\":{");
        
        boolean first = true;
        for (Map.Entry<String, List<String>> entry : fieldErrors.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;
            
            json.append("\"").append(entry.getKey()).append("\":[");
            boolean firstError = true;
            for (String error : entry.getValue()) {
                if (!firstError) {
                    json.append(",");
                }
                firstError = false;
                json.append("\"").append(error).append("\"");
            }
            json.append("]");
        }
        
        json.append("}}");
        return json.toString();
    }
}