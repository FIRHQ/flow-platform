package com.flowci.build.business;

import com.flowci.SpringTest;
import com.flowci.build.model.Build;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;

class WaitForAgentTest extends SpringTest {

    @Autowired
    private MockRepositoriesConfig mockRepositoriesConfig;

    @Autowired
    private WaitForAgent waitForAgent;

    @Test
    void givenBuildId_whenWaitForAgent_thenStatusShouldBeQueued() {
        var mockBuildRepo = mockRepositoriesConfig.getBuildRepo();
        var argCaptor = ArgumentCaptor.forClass(Build.Status.class);
        doNothing().when(mockBuildRepo).updateBuildStatusById(anyLong(), argCaptor.capture());

        waitForAgent.invoke(1L);
        assertThat(argCaptor.getValue()).isEqualTo(Build.Status.QUEUED);
    }
}
