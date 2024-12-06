package com.flowci.flow.repo;

import com.flowci.flow.model.Flow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlowRepo extends JpaRepository<Flow, Long> {
}
