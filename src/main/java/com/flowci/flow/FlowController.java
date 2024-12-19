package com.flowci.flow;

import com.flowci.common.exception.DuplicateException;
import com.flowci.common.exception.ExceptionUtils;
import com.flowci.common.validator.ValidId;
import com.flowci.flow.business.CreateFlow;
import com.flowci.flow.business.FetchFlow;
import com.flowci.flow.business.FetchTemplates;
import com.flowci.flow.business.ListFlows;
import com.flowci.flow.model.CreateFlowParam;
import com.flowci.flow.model.Flow;
import com.flowci.flow.model.YamlTemplate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.lang.Long.parseLong;

@Slf4j
@RestController
@RequestMapping("/v2/flows")
@AllArgsConstructor
public class FlowController {

    private final FetchTemplates fetchTemplates;

    private final CreateFlow createFlow;

    private final ListFlows listFlows;

    private final FetchFlow fetchFlow;

    @GetMapping("/{id}")
    public Flow getFlow(@PathVariable("id") @Valid @ValidId String id) {
        return fetchFlow.invoke(parseLong(id));
    }

    @GetMapping
    public List<Flow> getFlows(@RequestParam(required = false, defaultValue = "10000") @Valid @ValidId String parentId,
                               @RequestParam(required = false, defaultValue = "0") @Valid @Min(0) Integer page,
                               @RequestParam(required = false, defaultValue = "20") @Valid @Min(20) Integer size) {
        return listFlows.invoke(parseLong(parentId), PageRequest.of(page, size));
    }

    @GetMapping("/templates")
    public List<YamlTemplate> getTemplates() {
        return fetchTemplates.invoke();
    }

    @PostMapping
    public Flow createFlow(@RequestBody @Valid CreateFlowParam param) {
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
