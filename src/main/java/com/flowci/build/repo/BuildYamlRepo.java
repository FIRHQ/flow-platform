package com.flowci.build.repo;

import com.flowci.build.model.BuildYaml;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BuildYamlRepo extends JpaRepository<BuildYaml, Long> {
}
