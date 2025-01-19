package com.flowci.flow;

import com.flowci.common.exception.DuplicateException;
import com.flowci.common.exception.ExceptionUtils;
import com.flowci.common.validator.ValidId;
import com.flowci.common.validator.ValidName;
import com.flowci.flow.business.*;
import com.flowci.flow.model.CreateFlowParam;
import com.flowci.flow.model.Flow;
import com.flowci.flow.model.YamlTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.lang.Long.parseLong;

@Slf4j
@RestController
@RequestMapping("/v2/flows")
@AllArgsConstructor
@Tag(name = "flow")
public class FlowController {

    private final FetchTemplates fetchTemplates;
    private final CreateFlow createFlow;
    private final ListFlows listFlows;
    private final FetchFlow fetchFlow;
    private final FetchFlowYamlContent fetchFlowYamlContent;
    private final UpdateFlowYamlContent updateFlowYamlContent;

    @GetMapping("/{name}")
    public Flow getFlow(@PathVariable("name") @Valid @ValidName String name) {
        return fetchFlow.invoke(name);
    }

    @Operation(description = "get flow list by page")
    @GetMapping
    public List<Flow> getFlows(@RequestParam(required = false, name = "parentId", defaultValue = "10000")
                               @Valid @ValidId
                               String parentId) {
        return listFlows.invoke(parseLong(parentId));
    }

    @Operation(description = "create a new flow")
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

    @Operation(description = "get yaml templates")
    @GetMapping("/templates")
    public List<YamlTemplate> getTemplates() {
        return fetchTemplates.invoke();
    }

    @Operation(
            description = "fetch flow yaml return base64 encoded yaml content",
            parameters = @Parameter(name = "id", description = "flow id"),
            responses = @ApiResponse(
                    description = "base64 encoded yaml",
                    content = @Content(
                            examples = @ExampleObject(value = "c3RlcHM6CiAgLSBuYW1lOiBzdGVwXzE" +
                                    "KICAgIGNvbW1hbmRzOgogICAgICAtIG5hbWU6IHByaW50CiAgICAgICAgYmFz" +
                                    "aDogfAogICAgICAgICAgZWNobyAic3RlcCAxIG9uIGJhc2gi")
                    )
            )
    )
    @GetMapping(value = "/{name}/yaml", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getYaml(@PathVariable("name") @Valid @ValidName String name) {
        var flow = fetchFlow.invoke(name);
        return fetchFlowYamlContent.invoke(flow.getId(), true);
    }

    @Operation(
            description = "update yaml by flow id",
            parameters = @Parameter(name = "id", description = "flow id"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "base64 encoded yaml",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(value = "c3RlcHM6CiAgLSBuYW1lOiBzdGVwXzE" +
                                    "KICAgIGNvbW1hbmRzOgogICAgICAtIG5hbWU6IHByaW50CiAgICAgICAgYmFz" +
                                    "aDogfAogICAgICAgICAgZWNobyAic3RlcCAxIG9uIGJhc2gi")
                    )
            )
    )
    @PostMapping(value = "/{name}/yaml", consumes = MediaType.TEXT_PLAIN_VALUE)
    public void updateYaml(@PathVariable("name") @Valid @ValidName String name,
                           @RequestBody String b64Yaml) {
        var flow = fetchFlow.invoke(name);
        updateFlowYamlContent.invoke(flow.getId(), b64Yaml);
    }
}