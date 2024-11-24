package com.flowci.core.plugin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.common.exception.ArgumentException;
import com.flowci.common.exception.NotFoundException;
import com.flowci.core.common.domain.SourceWithDomain;
import com.flowci.core.common.git.GitClient;
import com.flowci.core.common.manager.ResourceManager;
import com.flowci.core.plugin.domain.PluginParserV2;
import com.flowci.core.plugin.domain.PluginV2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service("pluginService")
@ConditionalOnProperty(prefix = "app.plugin.v2.enable", havingValue = "true")
public class PluginServiceV2Impl implements PluginServiceV2 {

    private static final TypeReference<List<SourceWithDomain>> RepoListType = new TypeReference<>() {
    };

    private static final String PluginFileName = "plugin.yml";

    private static final String PluginFileNameAlt = "plugin.yaml";

    @Autowired
    private Path tmpDir;

    @Autowired
    private Path pluginDir;

    @Autowired
    private ResourceManager resourceManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskExecutor appTaskExecutor;

    @Override
    public void load(Resource repoUri) {
        try {
            var is = resourceManager.getResource(repoUri);
            var sourceList = objectMapper.readValue(is, RepoListType);

//            for (var src : sourceList) {
//                appTaskExecutor.execute(() -> {
//                    try {
//                        var tmpPath = clonePluginFromGit(src.getSource());
//
//                    } catch (Throwable e) {
//                        log.error(e.getMessage());
//                    }
//                });
//            }

        } catch (Throwable e) {
            log.warn("Unable to load plugin repo '{}' : {}", repoUri, e.getMessage());
        }
    }

//    private Path clonePluginFromGit(String repoUrl) throws IOException {
//        log.info("Start to load plugin from: {}", repoUrl);
//
//        try {
//            var client = new GitClient(repoUrl, null, null);
//            var tempDir = Paths.get(pluginDir.toString(), "tmp", UUID.randomUUID().toString());
//
//            log.debug("clone {} to temp dir: {}", repoUrl, tempDir);
//            client.klone(tempDir);
//
//            return tempDir;
//        } catch (Exception e) {
//            throw new IOException(e.getMessage());
//        }
//    }
//
//    private PluginV2 loadFromPluginYaml(String repoUrl, Path path) throws IOException {
//        var pluginFile = Paths.get(path.toString(), PluginFileName);
//        if (!Files.exists(pluginFile)) {
//            pluginFile = Paths.get(path.toString(), PluginFileNameAlt);
//            if (!Files.exists(pluginFile)) {
//                throw new NotFoundException("The 'plugin.yml' not found {}", repoUrl);
//            }
//        }
//
//        byte[] ymlInBytes = Files.readAllBytes(pluginFile);
//        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(ymlInBytes)) {
//            PluginV2 plugin = PluginParserV2.parse(inputStream);
//
//            if (!plugin.getVersion().equals(meta.getVersion())) {
//                throw new ArgumentException(
//                        "Plugin {0} version {1} not match the name defined in repo {2}",
//                        plugin.getName(), plugin.getVersion().toString(), plugin.getVersion().toString());
//            }
//
//            plugin.setMeta(meta);
//        }
//    }
//
//    /**
//     * Get plugin repo path: {plugin dir}/v2/{repo}/{version}
//     */
//    private Path getDir(String name, String version) {
//        return Paths.get(pluginDir.toString(), "v2", name, version);
//    }
}
