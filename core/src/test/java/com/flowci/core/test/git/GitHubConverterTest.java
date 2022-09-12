/*
 * Copyright 2018 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flowci.core.test.git;

import com.flowci.core.common.domain.GitSource;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.git.converter.GitHubConverter;
import com.flowci.core.git.converter.TriggerConverter;
import com.flowci.core.git.domain.*;
import com.flowci.core.git.domain.GitTrigger.GitEvent;
import com.flowci.core.test.SpringScenario;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.Optional;

/**
 * @author yang
 */
public class GitHubConverterTest extends SpringScenario {

    @Autowired
    private TriggerConverter gitHubConverter;

    @Test
    public void should_parse_ping_event() {
        InputStream stream = load("github/webhook_ping.json");

        Optional<GitTrigger> optional = gitHubConverter.convert(GitHubConverter.Ping, stream);
        Assert.assertTrue(optional.isPresent());

        GitPingTrigger trigger = (GitPingTrigger) optional.get();
        Assert.assertNotNull(trigger);

        Assert.assertTrue(trigger.getActive());
        Assert.assertTrue(trigger.getEvents().contains("pull_request"));
        Assert.assertTrue(trigger.getEvents().contains("push"));
        Assert.assertEquals("2019-08-23T20:35:35Z", trigger.getCreatedAt());
    }

    @Test
    public void should_parse_push_event() {
        InputStream stream = load("github/webhook_push.json");

        Optional<GitTrigger> optional = gitHubConverter.convert(GitHubConverter.PushOrTag, stream);
        Assert.assertTrue(optional.isPresent());

        // then: object properties should be parsed
        GitPushTrigger t = (GitPushTrigger) optional.get();
        Assert.assertEquals(GitEvent.PUSH, t.getEvent());
        Assert.assertEquals(GitSource.GITHUB, t.getSource());
        Assert.assertEquals("86284448", t.getRepoId());
        Assert.assertEquals(2, t.getNumOfCommit());
        Assert.assertEquals("second commit", t.getMessage());
        Assert.assertEquals("gy2006", t.getSender().getName());
        Assert.assertEquals("master", t.getRef());

        // then: verify commit data
        var commit1 = t.getCommits().get(0);
        Assert.assertEquals("01c3935c0e058eafb1a71da3b1da75dc35e69a9d", commit1.getId());
        Assert.assertEquals("first commit", commit1.getMessage());
        Assert.assertEquals("https://github.com/gy2006/ci-test/commit/01c3935c0e058eafb1a71da3b1da75dc35e69a9d", commit1.getUrl());
        Assert.assertEquals("2021-12-05T20:58:28+01:00", commit1.getTime());
        Assert.assertEquals("Yang Guo", commit1.getAuthor().getName());
        Assert.assertEquals("yang@Yangs-MacBook-Pro.local", commit1.getAuthor().getEmail());

        var commit2 = t.getCommits().get(1);
        Assert.assertEquals("410a0cda5875c3a1ede806e77c07be1382e2ebf3", commit2.getId());
        Assert.assertEquals("second commit", commit2.getMessage());
        Assert.assertEquals("https://github.com/gy2006/ci-test/commit/410a0cda5875c3a1ede806e77c07be1382e2ebf3", commit2.getUrl());
        Assert.assertEquals("2021-12-05T20:59:26+01:00", commit2.getTime());
        Assert.assertEquals("gy2006", commit2.getAuthor().getName());
        Assert.assertEquals("32008001@qq.com", commit2.getAuthor().getEmail());

        var vars = t.toVariableMap();
        Assert.assertEquals("master", vars.get(Variables.Git.BRANCH));
    }

    @Test
    public void should_parse_tag_event() {
        InputStream stream = load("github/webhook_tag.json");

        Optional<GitTrigger> optional = gitHubConverter.convert(GitHubConverter.PushOrTag, stream);
        Assert.assertTrue(optional.isPresent());
        Assert.assertTrue(optional.get() instanceof GitTagTrigger);

        GitTagTrigger t = (GitTagTrigger) optional.get();
        Assert.assertEquals(GitEvent.TAG, t.getEvent());
        Assert.assertEquals(GitSource.GITHUB, t.getSource());
        Assert.assertEquals("86284448", t.getRepoId());
        Assert.assertEquals("v2.1", t.getRef());
        Assert.assertEquals("second commit", t.getMessage());
        Assert.assertEquals("gy2006", t.getSender().getName());
        Assert.assertEquals("32008001@qq.com", t.getSender().getEmail());

        var vars = t.toVariableMap();
        Assert.assertEquals("v2.1", vars.get(Variables.Git.BRANCH));
    }

    @Test
    public void should_parse_pr_open_event() {
        InputStream stream = load("github/webhook_pr_open.json");

        Optional<GitTrigger> optional = gitHubConverter.convert(GitHubConverter.PR, stream);
        GitPrTrigger t = (GitPrTrigger) optional.get();
        Assert.assertNotNull(t);

        Assert.assertEquals(GitEvent.PR_OPENED, t.getEvent());
        Assert.assertEquals(GitSource.GITHUB, t.getSource());

        Assert.assertEquals("2", t.getNumber());
        Assert.assertEquals("Update settings.gradle", t.getTitle());
        Assert.assertEquals("pr...", t.getBody());
        Assert.assertEquals("2017-08-08T03:07:15Z", t.getTime());
        Assert.assertEquals("https://github.com/yang-guo-2016/Test/pull/2", t.getUrl());
        Assert.assertEquals("1", t.getNumOfCommits());
        Assert.assertEquals("1", t.getNumOfFileChanges());
        Assert.assertEquals(Boolean.FALSE, t.getMerged());

        Assert.assertEquals("8e7b8fb631ffcae6ae68338d0d16b381fdea4f31", t.getHead().getCommit());
        Assert.assertEquals("developer", t.getHead().getRef());
        Assert.assertEquals("yang-guo-2016/Test", t.getHead().getRepoName());
        Assert.assertEquals("https://github.com/yang-guo-2016/Test", t.getHead().getRepoUrl());

        Assert.assertEquals("ed6003bb96bd06cc75e38beb1176c5e9123ec607", t.getBase().getCommit());
        Assert.assertEquals("master", t.getBase().getRef());
        Assert.assertEquals("yang-guo-2016/Test", t.getBase().getRepoName());
        Assert.assertEquals("https://github.com/yang-guo-2016/Test", t.getBase().getRepoUrl());

        Assert.assertEquals("23307997", t.getSender().getId());
        Assert.assertEquals("yang-guo-2016", t.getSender().getUsername());

        var vars = t.toVariableMap();
        Assert.assertEquals("developer", vars.get(Variables.Git.BRANCH));
    }

    @Test
    public void should_parse_pr_close_event() {
        InputStream stream = load("github/webhook_pr_close.json");

        Optional<GitTrigger> optional = gitHubConverter.convert(GitHubConverter.PR, stream);
        GitPrTrigger t = (GitPrTrigger) optional.get();
        Assert.assertNotNull(t);

        Assert.assertNotNull(t);
        Assert.assertEquals(GitEvent.PR_MERGED, t.getEvent());
        Assert.assertEquals(GitSource.GITHUB, t.getSource());

        Assert.assertEquals("7", t.getNumber());
        Assert.assertEquals("Update settings.gradle title", t.getTitle());
        Assert.assertEquals("hello desc", t.getBody());
        Assert.assertEquals("2017-08-08T06:26:35Z", t.getTime());
        Assert.assertEquals("https://github.com/yang-guo-2016/Test/pull/7", t.getUrl());
        Assert.assertEquals("1", t.getNumOfCommits());
        Assert.assertEquals("1", t.getNumOfFileChanges());
        Assert.assertEquals(Boolean.TRUE, t.getMerged());

        Assert.assertEquals("1d1de876084ef656e522f360b88c1e96acf6b806", t.getHead().getCommit());
        Assert.assertEquals("developer", t.getHead().getRef());
        Assert.assertEquals("yang-guo-2016/Test", t.getHead().getRepoName());
        Assert.assertEquals("https://github.com/yang-guo-2016/Test", t.getHead().getRepoUrl());

        Assert.assertEquals("4e4e3750cd468f245bd9f0f938c4b5f76e1bc5b0", t.getBase().getCommit());
        Assert.assertEquals("master", t.getBase().getRef());
        Assert.assertEquals("yang-guo-2016/Test", t.getBase().getRepoName());
        Assert.assertEquals("https://github.com/yang-guo-2016/Test", t.getBase().getRepoUrl());

        Assert.assertEquals("23307997", t.getSender().getId());
        Assert.assertEquals("yang-guo-2016", t.getSender().getUsername());


        var vars = t.toVariableMap();
        Assert.assertEquals("master", vars.get(Variables.Git.BRANCH));
    }
}