package com.flowci.flow.model;

import com.flowci.common.model.EntityBase;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity(name = "flows")
@EqualsAndHashCode(callSuper = false, of = "id")
public final class Flow extends EntityBase {

    public final static Long ROOT_ID = 0L;

    public enum Type {
        GROUP,
        FLOW,
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "flows_id_gen")
    @SequenceGenerator(name = "flows_id_gen", sequenceName = "flows_id_sequence", allocationSize = 1)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Type type;

    private Long parentId = ROOT_ID;
}
