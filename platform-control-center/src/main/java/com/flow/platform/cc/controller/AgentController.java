/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.cc.controller;

import com.flow.platform.domain.AgentPathWithWebhook;
import com.flow.platform.cc.service.AgentService;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * @author gy@fir.im
 */
@RestController
@RequestMapping("/agents")
public class AgentController {

    private final AgentService agentService;

    @Autowired
    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * List online agents by zone name
     */
    @GetMapping(path = "/list")
    public Collection<Agent> list(@RequestParam(name = "zone", required = false) String zoneName) {
        return agentService.list(zoneName);
    }

    @GetMapping(path = "/list/online")
    public Collection<Agent> listOnline(@RequestParam(name = "zone", required = false) String zoneName) {
        return agentService.listForOnline(zoneName);
    }

    @GetMapping(path = "/find")
    public Agent find(@RequestParam(name = "zone") String zoneName, @RequestParam(name = "name") String agentName) {
        return agentService.find(new AgentPath(zoneName, agentName));
    }

    @PostMapping(path = "/create")
    public Agent create(@RequestBody AgentPathWithWebhook agentInfo) {
        if (agentInfo.isEmpty()) {
            throw new IllegalParameterException("miss required params ");
        }
        return agentService.create(agentInfo, agentInfo.getWebhook(), null);
    }

    @PostMapping(path = "/token/refresh")
    public String refreshToken(@RequestBody AgentPath agentPath) {
        if (agentPath.isEmpty()) {
            throw new IllegalParameterException("miss required params ");
        }
        return agentService.refreshToken(agentPath);
    }

    @GetMapping(path = "/settings")
    public AgentSettings getSettings(@RequestParam String token) {
        if (token == null) {
            throw new IllegalParameterException("miss required token");
        }
        return agentService.settings(token);
    }

    /**
     * Update agent status, required attributes are
     * - path
     * - status
     *
     * @param agent agent objc
     */
    @PostMapping(path = "/report")
    public void reportStatus(@RequestBody Agent agent) {
        if (agent.getPath() == null || agent.getStatus() == null) {
            throw new IllegalArgumentException("Agent path and status are required");
        }

        agentService.saveWithStatus(agent, agent.getStatus());
    }

    @PostMapping(path = "/delete")
    public void delete(@RequestBody Agent agent){
        agentService.delete(agent);
    }
}
