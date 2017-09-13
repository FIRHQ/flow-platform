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
package com.flow.platform.api.service.job;

import com.flow.platform.api.domain.job.JobYml;
import com.flow.platform.api.domain.node.NodeTree;
import java.math.BigInteger;

/**
 * @author lhl
 */
public interface JobNodeService {

    /**
     * save yml to db
     */
    void save(BigInteger jobId, String yml);


    /**
     * get node tree by job
     */
    NodeTree get(BigInteger jobId);

    /**
     * Get job yml data
     */
    JobYml find(BigInteger jobId);

}
