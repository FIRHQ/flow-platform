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

package com.flow.platform.cc.dao;

import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.util.DateUtil;
import com.google.common.collect.Sets;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Set;

/**
 * @author Will
 */
@Repository(value = "agentDao")
public class AgentDaoImpl extends AbstractBaseDao<AgentPath, Agent> implements AgentDao {

    private final Set<String> orderByFields = Sets
        .newHashSet("createdDate", "updatedDate", "sessionDate");

    @Override
    Class getEntityClass() {
        return Agent.class;
    }

    @Override
    public void update(final Agent obj) {
        execute(session -> {
            obj.setUpdatedDate(DateUtil.now());
            session.update(obj);
            return null;
        });
    }

    @Override
    public List<Agent> list(String zone, String orderByField, AgentStatus... status) {
        if (zone == null) {
            throw new IllegalArgumentException("Zone name is required");
        }

        if (orderByField != null && !orderByFields.contains(orderByField)) {
            throw new IllegalArgumentException(
                "The orderByField only availabe among 'createdDate', 'updateDate' or 'sessionDate'");
        }

        return (List<Agent>) execute(new Executable() {
            @Override
            public Object execute(Session session) {
                CriteriaBuilder builder = session.getCriteriaBuilder();
                CriteriaQuery<Agent> criteria = builder.createQuery(Agent.class);

                Root<Agent> root = criteria.from(Agent.class);
                criteria.select(root);

                Predicate whereCriteria = builder.equal(root.get("path").get("zone"), zone);

                if (status != null && status.length > 0) {
                    Predicate inStatus = root.get("status").in(status);
                    whereCriteria = builder.and(whereCriteria, inStatus);
                }
                criteria.where(whereCriteria);

                // order by created date
                if (orderByField != null) {
                    criteria.orderBy(builder.asc(root.get(orderByField)));
                }

                Query<Agent> query = session.createQuery(criteria);
                return query.getResultList();
            }
        });
    }

    @Override
    public Agent find(AgentPath agentPath) {
        Agent agent = (Agent) execute(session -> (Agent) session
            .createQuery("from Agent where AGENT_ZONE = :zone and AGENT_NAME = :name")
            .setParameter("zone", agentPath.getZone())
            .setParameter("name", agentPath.getName())
            .uniqueResult());
        return agent;
    }

    @Override
    public Agent find(String sessionId) {
        Agent agent = (Agent) execute(
            session -> (Agent) session.createQuery("from Agent where sessionId = :sessionId")
                .setParameter("sessionId", sessionId)
                .uniqueResult());
        return agent;
    }
}
