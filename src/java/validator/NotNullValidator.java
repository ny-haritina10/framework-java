package validator;

import engine.ConstraintValidator;
import engine.ValidationContext;

import validation.NotNull;

public class NotNullValidator implements ConstraintValidator<NotNull, Object> {
    
    @Override
    public void initialize(NotNull annotation) 
    { }
    
    @Override
    public boolean isValid(Object value, ValidationContext context) {
        return value != null; 
    }
}