package com.flowci.build;

import com.flowci.build.business.ListBuilds;
import com.flowci.build.model.Build;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v2/builds")
@Tag(name = "build")
@AllArgsConstructor
public class BuildController {

    private final ListBuilds listBuilds;

    @Operation(description = "list builds")
    @GetMapping
    public List<Build> getBuildsByStatus(@RequestParam("status") Build.Status status,
                                         @RequestParam(required = false, name = "page", defaultValue = "0")
                                         @Valid @Min(0)
                                         Integer page,
                                         @RequestParam(required = false, name = "size", defaultValue = "20")
                                         @Valid @Min(20)
                                         Integer size) {
        return listBuilds.invoke(status, PageRequest.of(page, size));
    }
}
