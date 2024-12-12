package com.flowci.common;

import com.flowci.common.validator.ValidName;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class NameValidatorTest {

    @ParameterizedTest
    @CsvSource({
            "abc_23,true",
            " ,false",
            "14-abc,true",
            "hello_world,true",
            "hello=world,false"
    })
    void shouldValidateName(String input, boolean expected) {
        var validator = new ValidName.NameValidator();
        var mockCtx = mock(ConstraintValidatorContext.class);

        assertEquals(expected, validator.isValid(input, mockCtx));
    }
}
