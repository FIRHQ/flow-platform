package com.flowci.flow.business;

import com.flowci.SpringTest;
import com.flowci.flow.model.Group;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InitRootGroupTest extends SpringTest {

    @Autowired
    private MockRepositoriesConfig repositoriesConfig;

    @Autowired
    private InitRootGroup initRootGroup;

    @Test
    void whenEmptyDatabase_thenDefaultRootGroupShouldBeCreated() {
        var groupRepoMock = repositoriesConfig.getGroupRepo();
        when(groupRepoMock.findById(any())).thenReturn(Optional.empty());

        var argCaptor = ArgumentCaptor.forClass(Group.class);
        when(groupRepoMock.save(argCaptor.capture())).thenReturn(mock(Group.class));

        initRootGroup.invoke();
        verify(groupRepoMock, times(1)).save(argCaptor.capture());

        var groupParam = argCaptor.getValue();
        assertEquals(Group.ROOT_NAME, groupParam.getName());
    }

    @Test
    void whenRootGroupExisted_thenSkipToCreateIt() {
        var groupRepoMock = repositoriesConfig.getGroupRepo();
        when(groupRepoMock.findById(any())).thenReturn(Optional.of(mock(Group.class)));

        initRootGroup.invoke();
        verify(groupRepoMock, times(0)).save(any(Group.class));
    }
}
