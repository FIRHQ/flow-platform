package com.flowci.flow.business.impl;

import com.flowci.common.RequestContextHolder;
import com.flowci.common.exception.NotAvailableException;
import com.flowci.flow.business.UpdateFlowYamlContent;
import com.flowci.flow.repo.FlowYamlRepo;
import com.flowci.yaml.business.ParseYamlV2;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;

import static java.lang.String.format;

@Slf4j
@Component
@AllArgsConstructor
public class UpdateFlowYamlContentImpl implements UpdateFlowYamlContent {

    private final ParseYamlV2 parseYamlV2;

    private final FlowYamlRepo flowYamlRepo;

    private final RequestContextHolder requestContextHolder;

    @Override
    public void invoke(Long id, String b64Yaml) {
        var yaml = new String(Base64.getDecoder().decode(b64Yaml));
        var ignore = parseYamlV2.invoke(yaml);

        var optional = flowYamlRepo.findById(id);
        if (optional.isEmpty()) {
            throw new NotAvailableException(format("flow %s not found", id));
        }

        var flowYaml = optional.get();
        flowYaml.setYaml(yaml);
        flowYaml.setUpdatedBy(requestContextHolder.getUserId());
        flowYamlRepo.save(flowYaml);
    }
}
