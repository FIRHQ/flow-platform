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

import com.flow.platform.util.Logger;
import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration file
 *
 * @author gy@fir.im
 */
@Configuration
@EnableRabbit
public class MQConfig {

    private final static Logger LOGGER = new Logger(MQConfig.class);

    private final static int MAX_PRIORITY = 10;
    private final static int MAX_LENGTH = 100;

    @Value("${mq.host}")
    private String host;

    @Value("${mq.queue.name}")
    private String cmdQueueName;

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        return factory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        template.setQueue(cmdQueueName);
        return template;
    }

    @Bean
    public AmqpAdmin amqpAdmin() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-max-length", MAX_LENGTH);
        args.put("x-max-priority", MAX_PRIORITY);
        Queue cmdQueue = new Queue(cmdQueueName, true, false, false, args);

        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory());
        rabbitAdmin.declareQueue(cmdQueue);
        return rabbitAdmin;
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        return new CachingConnectionFactory(host);
    }
}