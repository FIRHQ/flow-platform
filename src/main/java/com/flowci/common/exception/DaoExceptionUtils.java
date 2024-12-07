package com.flowci.common.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;

import java.util.Map;

public abstract class DaoExceptionUtils {

    // https://www.postgresql.org/docs/current/errcodes-appendix.html
    private final Map<Integer, DataAccessException> MAPPING = Map.of(
            23505, new DuplicateKeyException("duplicated key")
    );

    public BusinessException convertToBusinessExceptionIfPossible(Throwable e) {
        return null;
    }
}
