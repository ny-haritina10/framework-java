package engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationResult {
    
    private boolean valid = true;
    private Map<String, List<String>> errors = new HashMap<>();
    
    public void addError(String field, String message) {
        errors.computeIfAbsent(field, k -> new ArrayList<>()).add(message);
        valid = false;
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public Map<String, List<String>> getErrors() {
        return errors;
    }

    public void addErrors(Map<String, List<String>> errors) {
        for (Map.Entry<String, List<String>> entry : errors.entrySet()) {
            for (String error : entry.getValue()) {
                this.addError(entry.getKey(), error);
            }
        }
    }

    public void setIsValid(boolean valid)
    { this.valid = valid; }
}
