package com.flowci.common;


import com.flowci.common.exception.BusinessException;
import com.flowci.common.model.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import static com.flowci.common.exception.ExceptionUtils.findRootCause;
import static java.util.stream.Collectors.joining;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Slf4j
@ControllerAdvice
public final class GlobalExceptionHandler {

    @ResponseBody
    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorResponse> inputArgumentException(MethodArgumentNotValidException e) {
        var msg = e.getMessage();

        var fieldError = e.getBindingResult().getFieldError();
        if (fieldError != null && fieldError.getDefaultMessage() != null) {
            msg = fieldError.getDefaultMessage();
        }

        return ResponseEntity.status(BAD_REQUEST)
                .body(new ErrorResponse(BAD_REQUEST.value(), msg));
    }

    @ResponseBody
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> inputArgumentException(HandlerMethodValidationException e) {
        var message = e.getAllValidationResults()
                .stream()
                .map(r -> r.getResolvableErrors()
                        .stream()
                        .map(MessageSourceResolvable::getDefaultMessage)
                        .collect(joining("; ")))
                .collect(joining("; "));


        return ResponseEntity.status(BAD_REQUEST)
                .body(new ErrorResponse(BAD_REQUEST.value(), message));
    }

    @ResponseBody
    @ExceptionHandler({
            BusinessException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> onBusinessException(Throwable e) {
        return ResponseEntity.status(BAD_REQUEST)
                .body(new ErrorResponse(BAD_REQUEST.value(), e.getMessage()));
    }

    @ResponseBody
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> fatalException(Throwable e) {
        log.error("Fatal exception", e);
        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(INTERNAL_SERVER_ERROR.value(), findRootCause(e).getMessage()));
    }
}
