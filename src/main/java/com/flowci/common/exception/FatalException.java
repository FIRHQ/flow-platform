package com.flowci.common.exception;

public class FatalException extends RuntimeException {

    public FatalException(Throwable e) {
        super(e);
    }

    public FatalException(String message, Throwable e) {
        super(message, e);
    }
}
