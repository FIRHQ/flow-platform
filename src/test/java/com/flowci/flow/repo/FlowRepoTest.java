package com.flowci.flow.repo;

import com.flowci.SpringTestWithDB;
import com.flowci.flow.model.Flow;
import org.instancio.Instancio;
import org.instancio.InstancioApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

import java.time.Instant;

import static org.instancio.Select.all;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.*;

class FlowRepoTest extends SpringTestWithDB {

    @Autowired
    private FlowRepo flowRepo;

    @Test
    void givenFlow_whenSaving_thenIdAndTimestampCreated() {
        var flow = mockFlow().create();
        flowRepo.save(flow);

        assertNotNull(flow.getId());
        assertNotNull(flow.getCreatedAt());
        assertNotNull(flow.getUpdatedAt());

        var optional = flowRepo.findById(flow.getId());
        assertTrue(optional.isPresent());

        var fetched = optional.get();
        assertEquals(flow, fetched);
        assertEquals(flow.getCreatedAt(), fetched.getCreatedAt());
        assertEquals(flow.getUpdatedAt(), fetched.getUpdatedAt());
        assertEquals(flow.getVariables().size(), fetched.getVariables().size());
    }

    @Test
    void givenFlowsWithSameName_whenSaving_thenThrowDataAccessException() {
        var flow1 = mockFlow().create();
        flowRepo.save(flow1);

        var flow2 = mockFlow().set(field(Flow::getName), flow1.getName()).create();
        assertThrows(DataAccessException.class, () -> flowRepo.save(flow2));
    }

    private InstancioApi<Flow> mockFlow() {
        return Instancio.of(Flow.class)
                .ignore(field(Flow::getId))
                .ignore(all(Instant.class));
    }
}
