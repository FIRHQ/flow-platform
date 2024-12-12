package com.flowci.flow.repo;

import com.flowci.flow.model.FlowYaml;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlowYamlRepo extends JpaRepository<FlowYaml, Long> {
}
