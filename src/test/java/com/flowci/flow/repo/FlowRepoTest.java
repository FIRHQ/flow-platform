package com.flowci.flow.repo;

import com.flowci.SpringTestWithDB;
import com.flowci.flow.model.Flow;
import com.flowci.flow.model.FlowUser;
import org.instancio.InstancioApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;

import static com.flowci.TestUtils.newDummyInstance;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.*;

class FlowRepoTest extends SpringTestWithDB {

    @Autowired
    private FlowRepo flowRepo;

    @Autowired
    private FlowUserRepo flowUserRepo;

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

    @Test
    void whenFindFlowsByParentIdAndUserId_thenReturnListOfFlows() {
        var flow = flowRepo.save(mockFlow().create());

        var user1 = newDummyInstance(FlowUser.class)
                .set(field(FlowUser::getFlowId), flow.getId())
                .create();

        var user2 = newDummyInstance(FlowUser.class)
                .set(field(FlowUser::getFlowId), flow.getId())
                .create();

        flowUserRepo.save(user1);
        flowUserRepo.save(user2);

        var flowsForUser1 = flowRepo.findAllByParentIdAndUserIdOrderByCreatedAt(
                flow.getParentId(), user1.getUserId(), PageRequest.of(0, 1));
        assertEquals(1, flowsForUser1.size());

        var flowsForUser2 = flowRepo.findAllByParentIdAndUserIdOrderByCreatedAt(
                flow.getParentId(), user2.getUserId(), PageRequest.of(0, 1));
        assertEquals(1, flowsForUser2.size());
    }

    private InstancioApi<Flow> mockFlow() {
        return newDummyInstance(Flow.class).ignore(field(Flow::getId));
    }
}
