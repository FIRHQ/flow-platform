package com.flowci.flow.business;

import com.flowci.flow.model.CreateFlowParam;
import com.flowci.flow.model.Flow;

public interface CreateFlow {
    Flow invoke(CreateFlowParam param);
}
