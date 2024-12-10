package com.flowci.common;

import com.flowci.common.exception.DuplicateException;
import com.flowci.common.exception.FatalException;
import com.flowci.common.exception.NotAvailableException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static com.flowci.common.exception.ExceptionUtils.tryConvertToBusinessException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExceptionUtilsTest {

    @Test
    void whenThrowDataAccessException_thenConvertToDuplicatedExceptionAsExpected() {
        var sqlEx = mock(SQLException.class);
        when(sqlEx.getErrorCode()).thenReturn(23505);
        var dataAccessEx = new DataIntegrityViolationException("something wrong", sqlEx);

        assertThrows(DuplicateException.class, () -> {
            throw tryConvertToBusinessException(dataAccessEx, DuplicateException.class, "duplicated");
        });
    }

    @Test
    void whenThrowDataAccessException_thenThrowRuntimeExceptionIfExpectedExceptionNotAvailable() {
        var sqlEx = mock(SQLException.class);
        when(sqlEx.getErrorCode()).thenReturn(23505);
        var dataAccessEx = new DataIntegrityViolationException("something wrong", sqlEx);

        assertThrows(FatalException.class, () -> {
            throw tryConvertToBusinessException(dataAccessEx, NotAvailableException.class, "duplicated");
        });
    }
}
