package com.flowci.flow.model;

import com.flowci.common.model.EntityBase;
import com.flowci.common.model.Variables;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity(name = "groups")
@EqualsAndHashCode(callSuper = false, of = "id")
public class Group extends EntityBase {

    public final static Long ROOT_ID = 10000L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "groups_id_gen")
    @SequenceGenerator(name = "groups_id_gen", sequenceName = "groups_id_sequence", allocationSize = 1)
    private Long id;

    private String name;

    // parent group id
    private Long parentId;

    @Convert(converter = Variables.AttributeConverter.class)
    private Variables variables = new Variables();
}
