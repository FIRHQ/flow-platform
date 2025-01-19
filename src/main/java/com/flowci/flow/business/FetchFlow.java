package com.flowci.flow.business;

import com.flowci.flow.model.Flow;

public interface FetchFlow {
    Flow invoke(Long id);
    Flow invoke(String name);
}
