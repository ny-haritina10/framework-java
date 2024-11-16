package validation;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Size {
    int min() default 0;
    int max() default Integer.MAX_VALUE;
    String message() default "Size must be between {min} and {max}";
}