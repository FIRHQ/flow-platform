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

package com.flow.platform.api.service;

import com.flow.platform.api.domain.EmailSettingContent;
import com.flow.platform.api.domain.MessageType;
import com.flow.platform.api.domain.SettingContent;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobStatus;
import java.math.BigInteger;

/**
 * @author yh@firim
 */

public interface MessageService {

    /**
     * save message setting
     */
    SettingContent save(SettingContent t);

    /**
     * find message setting
     */
    SettingContent find(MessageType type);

    /**
     * delete message setting
     */
    void delete(SettingContent t);

    /**
     * update message setting
     */
    SettingContent update(SettingContent t);

    /**
     * test message setting
     */
    Boolean authEmailSetting(EmailSettingContent emailSetting);

    /**
     * send message to member
     */
    void sendMessage(Job job, JobStatus jobStatus);
}
