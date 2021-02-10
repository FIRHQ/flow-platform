package com.flowci.core.job.service;

import com.flowci.core.agent.domain.Agent;
import com.flowci.core.agent.domain.TtyCmd;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Step;
import com.flowci.core.job.event.TtyStatusUpdateEvent;
import com.flowci.exception.CIException;
import com.flowci.exception.StatusException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class TtyServiceImpl implements TtyService {

    @Autowired
    private JobService jobService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private StepService stepService;

    @Override
    public void execute(TtyCmd.In in) {
        try {
            Agent agent = getAgent(in.getId(), in.getNodePath());

            // dispatch cmd to agent,
            // response will be handled in JobEventService
            // std out/err log handled in LoggingService
            agentService.dispatch(in, agent);
        } catch (CIException e) {
            TtyCmd.Out out = new TtyCmd.Out()
                    .setId(in.getId())
                    .setAction(in.getAction())
                    .setSuccess(false)
                    .setError(e.getMessage());
            eventManager.publish(new TtyStatusUpdateEvent(this, out));
        }
    }

    private Agent getAgent(String jobId, String nodePath) {
        Job job = jobService.get(jobId);
        if (job.isDone()) {
            throw new StatusException("Cannot open tty since job is done");
        }

        Step step = stepService.get(jobId, nodePath);
        Agent agent = agentService.get(step.getAgentId());
        if (!agent.isBusy()) {
            throw new StatusException("Cannot open tty since agent not available");
        }

        return agent;
    }
}
