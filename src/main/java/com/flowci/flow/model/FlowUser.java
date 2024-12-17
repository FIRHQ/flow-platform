package com.flowci.flow.model;

import com.flowci.common.model.EntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "flows_user")
@IdClass(FlowUser.Id.class)
@EqualsAndHashCode(of = {"flowId", "userId"}, callSuper = false)
public class FlowUser extends EntityBase {

    public record Id(Long flowId, Long userId) {
    }

    @jakarta.persistence.Id
    private Long flowId;

    @jakarta.persistence.Id
    private Long userId;
}
