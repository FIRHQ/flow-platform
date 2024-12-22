package com.flowci.build.repo;

import com.flowci.build.model.Build;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BuildRepo extends JpaRepository<Build, Long> {
}
