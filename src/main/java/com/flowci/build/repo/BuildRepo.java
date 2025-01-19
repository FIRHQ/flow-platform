package com.flowci.build.repo;

import com.flowci.build.model.Build;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BuildRepo extends JpaRepository<Build, Long> {

    List<Build> findAllByStatusOrderByCreatedAtDesc(Build.Status status, Pageable pageable);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Build b set b.status = ?2 where b.id= ?1")
    void updateBuildStatusById(Long buildId, Build.Status status);
}
