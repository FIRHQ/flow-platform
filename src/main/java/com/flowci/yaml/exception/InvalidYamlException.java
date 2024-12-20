package com.flowci.yaml.exception;

import com.flowci.common.exception.BusinessException;

public class InvalidYamlException extends BusinessException {
    public InvalidYamlException(String message) {
        super(message);
    }
}
