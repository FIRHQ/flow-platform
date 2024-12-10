package com.flowci.common;

import com.flowci.common.model.RequestContext;
import org.springframework.stereotype.Component;

@Component
public class RequestContextHolder {

    private static final ThreadLocal<RequestContext> CONTEXT_HOLDER = new ThreadLocal<>();

    public RequestContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    public String getUser() {
        // TODO: user auth
        return "flowci";
    }
}
