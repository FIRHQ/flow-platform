package com.flowci.build.model;

import com.flowci.common.model.EntityBase;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = false, of = {"id"})
@Entity
@Table(name = "build")
public class Build extends EntityBase {

    public enum Trigger {
        MANUAL,
        API,
        SCHEDULED,
        GIT_PUSH,
        GIT_PR_OPEN,
        GIT_PR_CLOSE,
        GIT_TAG,
    }

    public enum Status {
        CREATED, // init status
        LOADING, // when need to loading yaml from git
        QUEUED, // been put to job queue and waiting for agent
        ASSIGNED, // assigned to an agent
        RUNNING, // agent start to execute the flow
        CANCELLING, // will be cancelled, but waiting for response from agent
        SUCCESS,
        FAILURE,
        CANCELLED,
        TIMEOUT
    }

    public static final Set<Status> FINAL_STATUS = Set.of(
            Status.SUCCESS,
            Status.FAILURE,
            Status.CANCELLED,
            Status.TIMEOUT
    );

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "build_id_gen")
    @SequenceGenerator(name = "build_id_gen", sequenceName = "build_id_sequence", allocationSize = 1)
    private Long id;

    private Long flowId;

    // auto generated from db, YYYYMMMDD as integer
    @Generated(event = EventType.INSERT)
    @Column(name = "build_date", insertable = false, updatable = false, nullable = false)
    private Integer buildDate;

    // auto generated from db, sequence ref to build date
    @Generated(event = EventType.INSERT)
    @Column(name = "build_sequence", insertable = false, updatable = false, nullable = false)
    private Long buildSequence;

    // auto generated from db, <buildDate>.<buildSequence>
    @Generated(event = EventType.INSERT)
    @Column(name = "build_alias", insertable = false, updatable = false, nullable = false)
    private String buildAlias;

    @Enumerated(EnumType.STRING)
    private Trigger trigger;

    @Enumerated(EnumType.STRING)
    private Status status;

    @org.hibernate.annotations.Type(StringArrayType.class)
    private String[] agentTags;

    @Nullable
    private String commitHash;

    /**
     * Assigned agent id
     */
    @Nullable
    private Long agentId;
}
