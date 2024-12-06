package com.flowci.flow;

import com.flowci.SpringTestWithDB;
import com.flowci.flow.model.Flow;
import com.flowci.flow.repo.FlowRepo;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.instancio.Select.all;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.*;

class FlowRepoTest extends SpringTestWithDB {

    @Autowired
    private FlowRepo flowRepo;

    @Test
    void givenFlow_whenSave_thenIdAndTimestampCreated() {
        var flow = Instancio.of(Flow.class)
                .ignore(field(Flow::getId))
                .ignore(all(Instant.class))
                .create();

        flowRepo.save(flow);

        assertNotNull(flow.getId());
        assertNotNull(flow.getCreatedAt());
        assertNotNull(flow.getUpdatedAt());


        var fetched = flowRepo.findById(flow.getId());
        assertTrue(fetched.isPresent());
        assertEquals(flow, fetched.get());
    }
}
