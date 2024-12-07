package com.flowci.flow;

import com.flowci.flow.model.Flow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/flows")
public class FlowController {

    @GetMapping("/{id}")
    public Flow getFlow(@PathVariable("id") Long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
