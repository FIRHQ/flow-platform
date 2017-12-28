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

package com.flow.platform.domain;

import com.google.gson.annotations.Expose;
import java.time.ZonedDateTime;

/**
 * @author gy@fir.im
 */
public class Agent extends Webhookable {

    /**
     * Composite key
     */
    @Expose
    private AgentPath path;

    /**
     * Max concurrent proc number
     */
    @Expose
    private Integer concurrentProc = 1;

    /**
     * Agent busy or idle
     */
    @Expose
    private AgentStatus status = AgentStatus.OFFLINE;

    /**
     * Reserved for session id
     */
    @Expose
    private String sessionId;

    /**
     * The date to start session
     */
    @Expose
    private ZonedDateTime sessionDate;

    /**
     * agent token
     */
    @Expose
    private String token;

    /**
     * Agent operation system name
     */
    @Expose
    private String os;

    /**
     * Created date
     */
    @Expose
    private ZonedDateTime createdDate;

    /**
     * Updated date
     */
    @Expose
    private ZonedDateTime updatedDate;

    public Agent() {
    }

    public Agent(String zone, String name) {
        this(new AgentPath(zone, name));
    }

    public Agent(AgentPath path) {
        this.path = path;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public AgentPath getPath() {
        return path;
    }

    public void setPath(AgentPath path) {
        this.path = path;
    }

    public String getZone() {
        return this.path.getZone();
    }

    public String getName() {
        return this.path.getName();
    }

    public Integer getConcurrentProc() {
        return concurrentProc;
    }

    public void setConcurrentProc(Integer concurrentProc) {
        this.concurrentProc = concurrentProc;
    }

    public AgentStatus getStatus() {
        return status;
    }

    public void setStatus(AgentStatus status) {
        this.status = status;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public ZonedDateTime getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(ZonedDateTime sessionDate) {
        this.sessionDate = sessionDate;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public ZonedDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(ZonedDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    public boolean isAvailable() {
        return getStatus() == AgentStatus.IDLE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Agent agent = (Agent) o;
        return path.equals(agent.getPath());
    }

    @Override
    public int hashCode() {
        return this.getPath().hashCode();
    }

    @Override
    public String toString() {
        return "Agent{" +
            "zone='" + path.getZone() + '\'' +
            ", name='" + path.getName() + '\'' +
            ", status=" + status + '\'' +
            ", sessionId=" + sessionId +
            '}';
    }
}
