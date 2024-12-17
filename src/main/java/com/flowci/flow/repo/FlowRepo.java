package com.flowci.flow.repo;

import com.flowci.flow.model.Flow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlowRepo extends JpaRepository<Flow, Long> {

    @Query("select f from Flow f " +
            "where f.id in (select fu.flowId from FlowUser fu where fu.userId = ?2) " +
            "and f.parentId = ?1 " +
            "order by f.createdAt")
    List<Flow> findAllByParentIdAndUserIdOrderByCreatedAt(
            Long parentId,
            Long userId,
            Pageable pageable
    );
}
