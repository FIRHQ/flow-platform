package com.flowci.flow;

import com.flowci.common.exception.DuplicateException;
import com.flowci.common.exception.ExceptionUtils;
import com.flowci.flow.business.CreateFlow;
import com.flowci.flow.business.FetchTemplates;
import com.flowci.flow.model.CreateFlowParam;
import com.flowci.flow.model.Flow;
import com.flowci.flow.model.YamlTemplate;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v2/flows")
public class FlowController {

    private final FetchTemplates fetchTemplates;

    private final CreateFlow createFlow;

    public FlowController(FetchTemplates fetchTemplates, CreateFlow createFlow) {
        this.fetchTemplates = fetchTemplates;
        this.createFlow = createFlow;
    }

    @GetMapping("/{id}")
    public Flow getFlow(@PathVariable("id") Long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @GetMapping("/templates")
    public List<YamlTemplate> getTemplates() {
        return fetchTemplates.invoke();
    }

    @PostMapping
    public Flow createFlow(@Valid @RequestBody CreateFlowParam param) {
        try {
            return createFlow.invoke(param);
        } catch (Throwable e) {
            throw ExceptionUtils.tryConvertToBusinessException(e,
                    DuplicateException.class,
                    "duplicate flow name"
            );
        }
    }
}
