package com.flowci.flow.model;

import com.flowci.common.model.EntityBase;
import com.flowci.common.model.Variables;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "flows")
@EqualsAndHashCode(callSuper = false, of = "id")
public final class Flow extends EntityBase {

    public static final Long ROOT_ID = 10000L;
    public static final String ROOT_NAME = "flows";

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

    // parent group id
    private Long parentId = ROOT_ID;

    @Convert(converter = Variables.AttributeConverter.class)
    private Variables variables;

    @Convert(converter = GitLink.Converter.class)
    private GitLink gitLink;
}
