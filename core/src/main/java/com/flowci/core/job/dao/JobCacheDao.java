package com.flowci.core.job.dao;

import com.flowci.core.job.domain.JobCache;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobCacheDao extends MongoRepository<JobCache, String> {

    Optional<JobCache> findByFlowIdAndKey(String flowId, String key);
}
