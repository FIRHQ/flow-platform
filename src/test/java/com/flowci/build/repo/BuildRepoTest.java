package com.flowci.build.repo;

import com.flowci.SpringTestWithDB;
import com.flowci.build.model.Build;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.flowci.TestUtils.newDummyInstance;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BuildRepoTest extends SpringTestWithDB {

    @Autowired
    private BuildRepo buildRepo;

    @Test
    void givenBuild_whenSaving_thenBuildIsSaved() {
        var mockFlowId = 100L;
        var build = newDummyInstance(Build.class)
                .set(field(Build::getFlowId), mockFlowId)
                .ignore(field(Build::getId))
                .ignore(field(Build::getBuildDate))
                .ignore(field(Build::getBuildSequence))
                .ignore(field(Build::getBuildAlias))
                .create();

        var saved = buildRepo.save(build);
        assertNotNull(saved.getBuildDate());
        assertNotNull(saved.getBuildSequence());
    }

    @Test
    void giveBuild_whenUpdateStatus_thenStatusIsUpdated() {
        var mockFlowId = 100L;
        var build = newDummyInstance(Build.class)
                .set(field(Build::getFlowId), mockFlowId)
                .ignore(field(Build::getId))
                .ignore(field(Build::getBuildDate))
                .ignore(field(Build::getBuildSequence))
                .ignore(field(Build::getBuildAlias))
                .create();

        var saved = buildRepo.save(build);
        assertNotNull(saved.getId());

        buildRepo.updateBuildStatusById(saved.getId(), Build.Status.SUCCESS);
        assertEquals(Build.Status.SUCCESS, buildRepo.findById(saved.getId()).get().getStatus());
    }
}
