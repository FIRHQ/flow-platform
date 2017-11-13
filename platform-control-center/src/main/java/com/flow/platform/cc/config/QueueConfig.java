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

package com.flow.platform.cc.config;

import com.flow.platform.cc.domain.CmdStatusItem;
import com.flow.platform.core.queue.InMemoryQueue;
import com.flow.platform.core.queue.PlatformQueue;
import com.flow.platform.core.queue.RabbitQueue;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.util.Logger;
import javax.annotation.PostConstruct;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * RabbitMQ configuration file
 *
 * @author gy@fir.im
 */
@Configuration
public class QueueConfig {

    public final static int CMD_QUEUE_MAX_LENGTH = 100;

    public final static int CMD_QUEUE_DEFAULT_PRIORITY = 10;

    public final static String PROP_CMD_QUEUE_RETRY = "queue.cmd.retry.enable";

    private final static Logger LOGGER = new Logger(QueueConfig.class);

    /**
     * Rabbit mq host
     * Example: amqp://guest:guest@localhost:5672
     */
    @Value("${mq.host}")
    private String host;

    /**
     * Rabbit mq management url
     * Example: http://localhost:15672
     */
    @Value("${mq.management.host}")
    private String mgrHost;

    /**
     * Cmd queue name for RabbitMQ
     */
    @Value("${queue.cmd.rabbit.name}")
    private String cmdQueueName;

    /**
     * Enable RabbitMQ or using embedded queue
     */
    @Value("${queue.cmd.rabbit.enable}")
    private Boolean cmdQueueRabbitEnable;

    /**
     * Enable cmd queue retry instead of pause/resume logic
     */
    @Value("${queue.cmd.retry.enable}")
    private Boolean cmdQueueRetryEnable;

    /**
     * AppConfig task executor
     */
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @PostConstruct
    public void init() {
        LOGGER.trace("Host: %s", host);
        LOGGER.trace("Management Host: %s", mgrHost);

        LOGGER.trace("Cmd queue name: %s", cmdQueueName);
        LOGGER.trace("Cmd RabbitMQ enabled: %s", cmdQueueRabbitEnable);
        LOGGER.trace("Cmd queue retry enabled: %s", cmdQueueRetryEnable);
    }

    @Bean
    public PlatformQueue<Message> cmdQueue() {
        if (cmdQueueRabbitEnable) {
            LOGGER.trace("Apply RabbitMQ for cmd queue");
            return new RabbitQueue(taskExecutor, host, CMD_QUEUE_MAX_LENGTH, CMD_QUEUE_DEFAULT_PRIORITY, cmdQueueName);
        }

        LOGGER.trace("Apply in memory queue for cmd queue");
        return new InMemoryQueue<>(taskExecutor, CMD_QUEUE_MAX_LENGTH, "CmdQueue");
    }

    /**
     * Queue to handle agent report online in sync
     */
    @Bean
    public PlatformQueue<AgentPath> agentReportQueue() {
        return new InMemoryQueue<>(taskExecutor, 100, "AgentReportQueue");
    }

    /**
     * Queue to handle cmd status update
     */
    @Bean
    public PlatformQueue<CmdStatusItem> cmdStatusQueue() {
        return new InMemoryQueue<>(taskExecutor, 100, "CmdStatusQueue");
    }
}