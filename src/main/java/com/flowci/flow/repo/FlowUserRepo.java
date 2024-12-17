package com.flowci.flow.repo;

import com.flowci.flow.model.FlowUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlowUserRepo extends JpaRepository<FlowUser, FlowUser.Id> {

    @Query(value = "select f.flowId from FlowUser f where f.userId = ?1")
    List<Long> findAllFlowIdsByUserId(Long userId);
}
