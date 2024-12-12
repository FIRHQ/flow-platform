package com.flowci.flow.model;

import com.flowci.common.model.EntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity(name = "flows_yaml")
@EqualsAndHashCode(callSuper = false, of = "id")
public class FlowYaml extends EntityBase {

    @Id
    private Long id;

    // raw yaml configuration
    private String yaml = "";
}
