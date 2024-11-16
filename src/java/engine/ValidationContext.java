package engine;

public class ValidationContext {
    
    private Object target;
    private ValidationResult result;
    
    public ValidationContext(Object target) {
        this.target = target;
        this.result = new ValidationResult();
    }
    
    public ValidationResult getResult() {
        return result;
    }
}