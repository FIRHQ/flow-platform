package com.flowci.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.springframework.util.StringUtils;

import java.lang.annotation.*;

@Constraint(validatedBy = ValidName.NameValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidName {

    String message() default "invalid name";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class NameValidator implements ConstraintValidator<ValidName, String> {

        private static final int MAX_LENGTH = 100;
        private static final String REGEX = "^[a-zA-Z0-9_\\-]+$";

        @Override
        public void initialize(ValidName constraintAnnotation) {
            ConstraintValidator.super.initialize(constraintAnnotation);
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return StringUtils.hasLength(value)
                    && value.length() <= MAX_LENGTH
                    && value.matches(REGEX);
        }
    }
}
