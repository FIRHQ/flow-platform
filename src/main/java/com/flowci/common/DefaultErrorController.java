package com.flowci.common;

import com.flowci.common.exception.BusinessException;
import com.flowci.common.module.ErrorResponse;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.Map;

@Hidden
@RestController
public class DefaultErrorController implements ErrorController {

    private static final Map<Class<?>, Integer> StatusCodeMapping =
            Map.of(BusinessException.class, HttpStatus.BAD_REQUEST.value());

    private final DefaultErrorAttributes errorAttributes;

    public DefaultErrorController(DefaultErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping("/error")
    public ResponseEntity<ErrorResponse> handleError(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        var error = errorAttributes.getError(new ServletWebRequest(request));
        var statusCode = StatusCodeMapping.getOrDefault(error.getClass(), response.getStatus());

        return ResponseEntity
                .status(statusCode)
                .body(new ErrorResponse(statusCode, error.getMessage()));
    }
}
