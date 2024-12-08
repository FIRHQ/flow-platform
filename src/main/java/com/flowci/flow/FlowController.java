package com.flowci.flow;

import com.flowci.flow.business.FetchTemplates;
import com.flowci.flow.model.Flow;
import com.flowci.flow.model.YamlTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v2/flows")
public class FlowController {

    public final FetchTemplates fetchTemplates;

    public FlowController(FetchTemplates fetchTemplates) {
        this.fetchTemplates = fetchTemplates;
    }

    @GetMapping("/{id}")
    public Flow getFlow(@PathVariable("id") Long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @GetMapping("/templates")
    public List<YamlTemplate> getTemplates() {
        return fetchTemplates.invoke();
    }
}
