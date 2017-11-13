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

package com.flow.platform.api.git;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.envs.GitEnvs;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.util.Logger;
import com.flow.platform.util.git.GitClient;
import com.flow.platform.util.git.GitSshClient;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Build SSH git client with FLOW_GIT_SSH_PRIVATE_KEY and FLOW_GIT_SSH_PUBLIC_KEY
 *
 * @author yang
 */
public class GitSshClientBuilder extends GitClientBuilder {

    private final static Logger LOGGER = new Logger(GitSshClientBuilder.class);

    private final static String SSH_PRIVATE_KEY_NAME = "ssh_private_key";

    private String privateKey;

    private String publicKey;

    public GitSshClientBuilder(final Node node, final Path sourceFolder) {
        super(node, sourceFolder);
        privateKey = node.getEnv(GitEnvs.FLOW_GIT_SSH_PRIVATE_KEY);
        publicKey = node.getEnv(GitEnvs.FLOW_GIT_SSH_PUBLIC_KEY);
    }

    @Override
    public GitClient build() {
        GitSshClient client = new GitSshClient(url, sourceFolder);

        // save private key to flow workspace folder
        if (!Strings.isNullOrEmpty(privateKey)) {
            Path privateKeyPath = Paths.get(sourceFolder.getParent().toString(), SSH_PRIVATE_KEY_NAME);

            try {
                Files.write(privateKey, privateKeyPath.toFile(), AppConfig.DEFAULT_CHARSET);
                client.setPrivateKeyPath(privateKeyPath);
            } catch (IOException warn) {
                LOGGER.warn("Fail to write ssh private key to: %s", privateKeyPath);
            }
        }
        
        return client;
    }
}
