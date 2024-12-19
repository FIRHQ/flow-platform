package com.flowci.flow.business;

import com.flowci.SpringTest;
import com.flowci.common.RequestContextHolder;
import com.flowci.flow.model.Flow;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ListFlowsTest extends SpringTest {

    @Autowired
    private MockRepositoriesConfig mockRepositoriesConfig;

    @Autowired
    private ListFlows listFlows;

    @MockBean
    private RequestContextHolder requestContextHolder;

    @Test
    void givenParentId_whenFetching_thenReturnAllFlowsUnderTheParent() {
        var flowRepoMock = mockRepositoriesConfig.getFlowRepo();
        var parentIdCaptor = ArgumentCaptor.forClass(Long.class);
        var userIdCaptor = ArgumentCaptor.forClass(Long.class);
        when(flowRepoMock.findAllByParentIdAndUserIdOrderByCreatedAt(
                parentIdCaptor.capture(),
                userIdCaptor.capture(),
                any(Pageable.class))
        ).thenReturn(Collections.emptyList());

        var userIdMock = 1L;
        when(requestContextHolder.getUserId()).thenReturn(userIdMock);

        listFlows.invoke(null, PageRequest.of(0, 1));

        assertEquals(Flow.ROOT_ID, parentIdCaptor.getValue());
        verify(flowRepoMock, times(1)).findAllByParentIdAndUserIdOrderByCreatedAt(
                parentIdCaptor.capture(), userIdCaptor.capture(), any(Pageable.class));
    }
}
