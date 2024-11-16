package engine;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import exception.ValidationException;
import validation.Size;
import validation.Valid;
import validator.SizeValidator;

public class ValidationEngine {
    private static Map<Class<? extends Annotation>, Class<? extends ConstraintValidator>> validators = new HashMap<>();
    
    static {
        // Register default validators
        validators.put(Size.class, SizeValidator.class);

        // TODO: Need Implementation
        // validators.put(NotNull.class, NotNullValidator.class);
        // validators.put(Pattern.class, PatternValidator.class);
    }
    
    public static ValidationResult validate(Object object) 
        throws ValidationException 
    {
        ValidationContext context = new ValidationContext(object);
        
        if (object.getClass().isAnnotationPresent(Valid.class)) {
            for (Field field : object.getClass().getDeclaredFields()) {
                validateField(field, object, context);
            }
        }
        
        return context.getResult();
    }
    
    private static void validateField(Field field, Object object, ValidationContext context) 
        throws ValidationException 
    {
        field.setAccessible(true);
        
        for (Annotation annotation : field.getAnnotations()) {
            Class<? extends ConstraintValidator> validatorClass = validators.get(annotation.annotationType());
            if (validatorClass != null) {
                try {
                    ConstraintValidator validator = validatorClass.getDeclaredConstructor().newInstance();
                    validator.initialize(annotation);
                    
                    Object value = field.get(object);
                    if (!validator.isValid(value, context)) {
                        String message = getValidationMessage(annotation);
                        context.getResult().addError(field.getName(), message);
                        context.getResult().setIsValid(false);
                    }
                } 
                
                catch (Exception e) {
                    throw new ValidationException("Error validating field: " + field.getName());
                }
            }
        }
    }

    private static String getValidationMessage(Annotation annotation) {
        try {
            Method message = annotation.annotationType().getDeclaredMethod("message");
            return (String) message.invoke(annotation);
        } catch (Exception e) {
            return "Validation failed";
        }
    }
}