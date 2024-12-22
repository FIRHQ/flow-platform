package com.flowci.build.model;

import com.flowci.common.model.EntityBase;
import com.flowci.common.model.Variables;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false, of = "buildId")
@Entity
@Table(name = "build_yaml")
public class BuildYaml extends EntityBase {

    @Id
    private Long id;

    // ref to flow variables
    @Convert(converter = Variables.AttributeConverter.class)
    private Variables variables;

    private String yaml;
}
