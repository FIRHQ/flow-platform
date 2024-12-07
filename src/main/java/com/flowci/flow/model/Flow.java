package com.flowci.flow.model;

import com.flowci.common.model.EntityBase;
import com.flowci.common.model.Variables;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity(name = "flows")
@EqualsAndHashCode(callSuper = false, of = "id")
public final class Flow extends EntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "flows_id_gen")
    @SequenceGenerator(name = "flows_id_gen", sequenceName = "flows_id_sequence", allocationSize = 1)
    private Long id;

    private String name;

    // parent group id
    private Long parentId = Group.ROOT_ID;

    @Convert(converter = Variables.AttributeConverter.class)
    private Variables variables = new Variables();

    @Convert(converter = GitLink.Converter.class)
    private GitLink gitLink;

    private int maxQueueTime = 3600; // in seconds

    private int stepTimeout = 1800; // job step timeout in second;

    private String cron;
}
