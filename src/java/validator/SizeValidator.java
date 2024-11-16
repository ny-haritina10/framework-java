package validator;

import engine.ConstraintValidator;
import engine.ValidationContext;

import validation.Size;

public class SizeValidator implements ConstraintValidator<Size, String> {
    
    private int min;
    private int max;
    
    @Override
    public void initialize(Size annotation) {
        this.min = annotation.min();
        this.max = annotation.max();
    }
    
    @Override
    public boolean isValid(String value, ValidationContext context) {
        if (value == null) return true;     // null handling should be done by @NotNull
        int length = value.length();
        return length >= min && length <= max;
    }
}