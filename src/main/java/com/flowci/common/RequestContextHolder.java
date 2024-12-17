package com.flowci.common;

import com.flowci.common.model.RequestContext;
import com.flowci.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class RequestContextHolder {

    private static final ThreadLocal<RequestContext> CONTEXT_HOLDER = new ThreadLocal<>();

    public RequestContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    public Long getUserId() {
        // TODO: user auth
        return User.SYSTEM_USER;
    }
}
