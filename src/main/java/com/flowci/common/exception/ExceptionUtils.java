package com.flowci.common.exception;

import org.springframework.dao.DataAccessException;

import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

public abstract class ExceptionUtils {

    // https://www.postgresql.org/docs/current/errcodes-appendix.html
    private static final Map<String, Class<? extends BusinessException>> MAPPING = Map.of(
            "23505", DuplicateException.class
    );

    /**
     * Try to convert Throwable with expected message
     * it will throw RuntimeException if you cannot convert
     */
    public static <T extends BusinessException> RuntimeException tryConvertToBusinessException(
            Throwable origin, Class<T> onClass, String message) {

        if (origin instanceof DataAccessException diEx) {
            if (findRootCause(diEx) instanceof SQLException sqlEx) {
                var klass = MAPPING.get(sqlEx.getSQLState());
                if (Objects.equals(klass, onClass)) {
                    return newBusinessException(klass, message);
                }
            }
        }

        return new FatalException(origin);
    }

    private static <T extends BusinessException> T newBusinessException(Class<T> klass, String message) {
        try {
            return klass.getDeclaredConstructor(String.class).newInstance(message);
        } catch (Throwable ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static Throwable findRootCause(Throwable t) {
        if (t.getCause() != null) {
            return findRootCause(t.getCause());
        }
        return t;
    }
}
