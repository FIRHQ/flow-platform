package com.flowci.build.model;

import com.flowci.common.model.EntityBase;
import com.flowci.common.model.Variables;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;

@Data
@Entity
@Table(name = "build_yaml")
@EqualsAndHashCode(callSuper = false, of = "id")
public class BuildYaml extends EntityBase {

    @Id
    private Long id;

    // ref to flow variables
    @Type(JsonType.class)
    private Variables variables;

    private String yaml;
}
