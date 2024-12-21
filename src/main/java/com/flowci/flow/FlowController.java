package com.flowci.flow;

import com.flowci.common.exception.DuplicateException;
import com.flowci.common.exception.ExceptionUtils;
import com.flowci.common.validator.ValidId;
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
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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

    @GetMapping("/{id}")
    public Flow getFlow(@PathVariable("id") @Valid @ValidId String id) {
        return fetchFlow.invoke(parseLong(id));
    }

    @GetMapping
    public List<Flow> getFlows(@RequestParam(required = false, name = "parentId", defaultValue = "10000")
                               @Valid @ValidId
                               String parentId,

                               @RequestParam(required = false, name = "page", defaultValue = "0")
                               @Valid @Min(0)
                               Integer page,

                               @RequestParam(required = false, name = "size", defaultValue = "20")
                               @Valid @Min(20)
                               Integer size) {
        return listFlows.invoke(parseLong(parentId), PageRequest.of(page, size));
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
    @GetMapping(value = "/{id}/yaml", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getYaml(@PathVariable("id") @Valid @ValidId String id) {
        return fetchFlowYamlContent.invoke(parseLong(id));
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
    @PostMapping(value = "/{id}/yaml", consumes = MediaType.TEXT_PLAIN_VALUE)
    public void updateYaml(@PathVariable("id") @Valid @ValidId String id,
                           @RequestBody String b64Yaml) {
        updateFlowYamlContent.invoke(parseLong(id), b64Yaml);
    }
}
