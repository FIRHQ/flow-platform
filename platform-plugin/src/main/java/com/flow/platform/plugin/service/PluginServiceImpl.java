/*
 * Copyright 2017 flow.ci
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

package com.flow.platform.plugin.service;

import static com.flow.platform.plugin.domain.PluginStatus.DELETE;
import static com.flow.platform.plugin.domain.PluginStatus.INSTALLED;
import static com.flow.platform.plugin.domain.PluginStatus.INSTALLING;
import static com.flow.platform.plugin.domain.PluginStatus.IN_QUEUE;
import static com.flow.platform.plugin.domain.PluginStatus.PENDING;

import com.flow.platform.plugin.dao.PluginDao;
import com.flow.platform.plugin.domain.Plugin;
import com.flow.platform.plugin.domain.PluginStatus;
import com.flow.platform.plugin.event.PluginRefreshEvent;
import com.flow.platform.plugin.event.PluginRefreshEvent.Status;
import com.flow.platform.plugin.event.PluginStatusChangeEvent;
import com.flow.platform.plugin.exception.PluginException;
import com.flow.platform.util.ExceptionUtil;
import com.flow.platform.util.Logger;
import com.flow.platform.util.git.GitException;
import com.flow.platform.util.git.JGitUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * @author yh@firim
 */
@Service
public class PluginServiceImpl extends ApplicationEventService implements PluginService {

    private final static String GIT_SUFFIX = ".git";

    private final static String LOCAL_REMOTE = "local";

    private final static String ORIGIN_REMOTE = "origin";

    private final static int REFRESH_CACHE_TASK_HEARTBEAT = 2 * 60 * 60 * 1000;

    private final static Logger LOGGER = new Logger(PluginService.class);

    // git clone folder
    @Autowired
    private Path gitWorkspace;

    // local library
    @Autowired
    private Path gitCacheWorkspace;

    @Autowired
    private ThreadPoolTaskExecutor pluginPoolExecutor;

    @Autowired
    private PluginDao pluginDao;

    @Autowired
    private String pluginSourceUrl;

    private final Map<Plugin, Future<?>> taskCache = new ConcurrentHashMap<>();

    private final List<Processor> processors = ImmutableList.of(
        new InitGitProcessor(),
        new FetchProcessor(),
        new PushProcessor());

    @Override
    public Plugin find(String name) {
        return pluginDao.get(name);
    }

    @Override
    public Collection<Plugin> list(PluginStatus... statuses) {
        return pluginDao.list(statuses);
    }

    @Override
    public void install(String pluginName) {
        Plugin plugin = find(pluginName);

        if (Objects.isNull(plugin)) {
            throw new PluginException("not found plugin, please ensure the plugin name is exist");
        }

        // not finish can install plugin
        if (!Plugin.RUNNING_AND_FINISH_STATUS.contains(plugin.getStatus())) {
            LOGGER.trace(String.format("Plugin %s Enter To Queue", pluginName));

            // update plugin status
            updatePluginStatus(plugin, IN_QUEUE);

            // record future task
            Future<?> submit = pluginPoolExecutor.submit(new InstallRunnable(plugin));
            taskCache.put(plugin, submit);
            LOGGER.trace(String.format("Plugin %s finish To Queue", pluginName));
        }
    }

    @Override
    public void stop(String name) {
        Plugin plugin = find(name);

        if (Objects.isNull(plugin)) {
            throw new PluginException("not found plugin, please ensure the plugin name is exist");
        }

        if (!ImmutableSet.of(IN_QUEUE, INSTALLING).contains(plugin.getStatus())) {
            throw new PluginException("Sorry can not stop");
        }

        try {
            Future<?> submit = taskCache.get(plugin);
            if (!Objects.isNull(submit)) {
                submit.cancel(true);
            } else {
                plugin.setStopped(true);
            }
        } catch (Throwable e) {
            LOGGER.warn("Cannot cancel future: " + e.getMessage());
        } finally {
            // update plugin status
            updatePluginStatus(plugin, PENDING);
            taskCache.remove(plugin);
        }
    }

    @Override
    public void uninstall(String name) {
        Plugin plugin = find(name);

        if (Objects.isNull(plugin)) {
            throw new PluginException("not found plugin, please ensure the plugin name is exist");
        }

        // Running Plugin not uninstall
        if (Objects.equals(plugin.getStatus(), INSTALLING)) {
            throw new PluginException("running plugin not install");
        }

        // only finish to uninstall
        if (!Plugin.FINISH_STATUSES.contains(plugin.getStatus())) {
            throw new PluginException("running plugin not install");
        }

        for (Processor processor : processors) {
            processor.clean(plugin);
        }

        // update plugin status to PENDING therefore the status been reset
        updatePluginStatus(plugin, DELETE);
    }

    @Override
    public void execInstallOrUpdate(Plugin plugin) {
        try {
            // update plugin status to INSTALLING
            updatePluginStatus(plugin, INSTALLING);

            for (Processor processor : processors) {
                processor.exec(plugin);
            }
        } catch (PluginException e) {
            plugin.setReason(ExceptionUtil.findRootCause(e).getMessage());
            updatePluginStatus(plugin, PENDING);
        } finally {
            taskCache.remove(plugin);
        }
    }

    @Override
    @Scheduled(fixedDelay = REFRESH_CACHE_TASK_HEARTBEAT)
    public void syncTask() {
        try {
            LOGGER.traceMarker("scheduleRefreshCache", "Start Refresh Cache");
            dispatchEvent(new PluginRefreshEvent(this, pluginSourceUrl, Status.ON_PROGRESS));
            pluginDao.refreshCache();
        } catch (Throwable e) {
            LOGGER.warn(e.getMessage());
        } finally {
            dispatchEvent(new PluginRefreshEvent(this, pluginSourceUrl, Status.IDLE));
            LOGGER.traceMarker("scheduleRefreshCache", "Finish Refresh Cache");
        }
    }

    private void updatePluginStatus(Plugin plugin, PluginStatus target) {
        switch (target) {
            case PENDING:
            case IN_QUEUE:
            case INSTALLING:
            case INSTALLED:
                plugin.setStatus(target);
                break;

            case DELETE:
                plugin.setStatus(PENDING);
                break;
        }

        pluginDao.update(plugin);
        dispatchEvent(new PluginStatusChangeEvent(this, plugin.getName(), plugin.getTag(), target));
    }

    /**
     * Git bare repos workspace
     */
    private Path gitRepoPath(Plugin plugin) {
        return Paths.get(gitWorkspace.toString(), plugin.getName() + GIT_SUFFIX);
    }

    /**
     * Build git clone path which clone repo from remote
     */
    private Path gitCachePath(Plugin plugin) {
        return Paths.get(gitCacheWorkspace.toString(), plugin.getName());
    }

    private interface Processor {

        void exec(Plugin plugin);

        void clean(Plugin plugin);
    }

    private class InitGitProcessor implements Processor {

        private final static String EMPTY_FILE = "empty.file";

        @Override
        public void exec(Plugin plugin) {
            LOGGER.traceMarker("InitGitProcessor", "Start Init Git");
            try {
                // init bare
                Path cachePath = gitCachePath(plugin);
                Path localPath = gitRepoPath(plugin);

                JGitUtil.init(cachePath, false);
                JGitUtil.init(localPath, true);

                // remote set
                JGitUtil.remoteSet(cachePath, ORIGIN_REMOTE, plugin.getDetails() + GIT_SUFFIX);
                JGitUtil.remoteSet(cachePath, LOCAL_REMOTE, localPath.toString());

                // if branch not exists then push branch
                if (!checkExistBranchOrNot(localPath)) {
                    LOGGER.traceMarker("InitGitProcessor", "Not Found Branch Create Empty Branch");
                    commitSomething(cachePath);
                    JGitUtil.push(cachePath, LOCAL_REMOTE, "master");
                }
            } catch (Throwable e) {
                LOGGER.error("Git Init", e);
                throw new PluginException("Git Init", e);
            }
        }

        @Override
        public void clean(Plugin plugin) {
            try {
                FileUtils.deleteDirectory(gitCachePath(plugin).toFile());
                FileUtils.deleteDirectory(gitRepoPath(plugin).toFile());
            } catch (Throwable e) {
                LOGGER.error("Git Init Clean", e);
                throw new PluginException("Git Init Clean", e);
            }
        }

        private void commitSomething(Path path) {
            try (Git git = Git.open(path.toFile())) {
                Path emptyFilePath = Paths.get(path.toString(), EMPTY_FILE);
                Files.createFile(emptyFilePath);

                git.add()
                    .addFilepattern(".")
                    .call();

                git.commit()
                    .setMessage("add test branch")
                    .call();

            } catch (Throwable e) {
                LOGGER.error("Method: commitSomething Exception", e);
            }
        }

        private boolean checkExistBranchOrNot(Path path) {
            try (Git git = Git.open(path.toFile())) {
                if (git.branchList().call().isEmpty()) {
                    return false;
                }
            } catch (Throwable e) {
                LOGGER.error("Method: checkExistBranchOrNot Exception", e);
            }
            return true;
        }
    }

    private class FetchProcessor implements Processor {

        @Override
        public void exec(Plugin plugin) {
            LOGGER.traceMarker("FetchProcessor", "Start Fetch Tags");
            try {
                JGitUtil.fetchTags(gitCachePath(plugin), ORIGIN_REMOTE);
            } catch (Throwable e) {
                LOGGER.error("Git Fetch", e);
                throw new PluginException("Git Fetch", e);
            }
        }

        @Override
        public void clean(Plugin plugin) {

        }
    }

    private class PushProcessor implements Processor {

        @Override
        public void exec(Plugin plugin) {
            LOGGER.traceMarker("PushProcessor", "Start Push Tags");
            try {
                // put from cache to local git workspace
                Path cachePath = gitCachePath(plugin);
                String latestGitTag = plugin.getTag();
                JGitUtil.push(cachePath, LOCAL_REMOTE, latestGitTag);

                updatePluginStatus(plugin, INSTALLED);
            } catch (GitException e) {
                LOGGER.error("Git Push", e);
                throw new PluginException("Git Push", e);
            }
        }

        @Override
        public void clean(Plugin plugin) {

        }
    }

    private class InstallRunnable implements Runnable {

        private Plugin plugin;

        public InstallRunnable(Plugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void run() {
            if (Objects.equals(false, plugin.getStopped())) {
                LOGGER.traceMarker("InstallRunnable", "Plugin Start Install Or Update");
                execInstallOrUpdate(plugin);
                LOGGER.traceMarker("InstallRunnable", "Plugin Finish Install Or Update");
                return;
            }

            plugin.setStopped(false);
            plugin.setStatus(PluginStatus.PENDING);
            pluginDao.update(plugin);
            LOGGER.traceMarker("InstallRunnable", "Plugin Stopped");
        }
    }
}
