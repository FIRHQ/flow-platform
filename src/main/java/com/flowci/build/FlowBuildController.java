package com.flowci.build;

import com.flowci.build.business.TriggerBuild;
import com.flowci.build.model.Build;
import com.flowci.common.model.Variables;
import com.flowci.common.validator.ValidName;
import com.flowci.flow.business.FetchFlow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v2/flows")
@Tag(name = "build for flows")
@AllArgsConstructor
public class FlowBuildController {

    private final FetchFlow fetchFlow;
    private final TriggerBuild triggerBuild;

    @Operation(
            description = "create new build for flow",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(schema = @Schema(implementation = Variables.class))
            )
    )
    @PostMapping("/{name}/build/trigger")
    public Build triggerBuild(@PathVariable("name") @Valid @ValidName String name,
                              @RequestBody(required = false) Variables inputs) {
        var flow = fetchFlow.invoke(name);
        return triggerBuild.invoke(flow.getId(), Build.Trigger.API, inputs);
    }
}
