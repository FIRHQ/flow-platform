package com.flowci.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Constraint(validatedBy = ValidId.IdValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidId {

    String message() default "invalid id";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class IdValidator implements ConstraintValidator<ValidId, String> {

        @Override
        public void initialize(ValidId constraintAnnotation) {
            ConstraintValidator.super.initialize(constraintAnnotation);
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            try {
                return Long.parseLong(value) > 0L;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }
}
