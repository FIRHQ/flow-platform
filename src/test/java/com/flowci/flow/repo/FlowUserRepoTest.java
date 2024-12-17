package com.flowci.flow.repo;

import com.flowci.SpringTestWithDB;
import com.flowci.flow.model.FlowUser;
import com.flowci.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowUserRepoTest extends SpringTestWithDB {

    @Autowired
    private FlowUserRepo flowUserRepo;

    @Test
    void whenFindByUserId_thenReturnListOfFlowId() {
        var userId = 100L;

        var f1 = new FlowUser();
        f1.setFlowId(1L);
        f1.setUserId(userId);
        f1.setCreatedBy(User.SYSTEM_USER);
        f1.setUpdatedBy(User.SYSTEM_USER);

        var f2 = new FlowUser();
        f2.setFlowId(2L);
        f2.setUserId(userId);
        f2.setCreatedBy(User.SYSTEM_USER);
        f2.setUpdatedBy(User.SYSTEM_USER);

        flowUserRepo.save(f1);
        flowUserRepo.save(f2);

        var listOfFlowId = flowUserRepo.findAllFlowIdsByUserId(userId);
        assertEquals(2, listOfFlowId.size());
        assertTrue(listOfFlowId.contains(f1.getFlowId()));
        assertTrue(listOfFlowId.contains(f2.getFlowId()));
    }
}
