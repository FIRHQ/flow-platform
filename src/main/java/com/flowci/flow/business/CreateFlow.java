package com.flowci.flow.business;

import com.flowci.flow.model.CreateFlowParam;

public interface CreateFlow {
    Long invoke(CreateFlowParam param);
}
