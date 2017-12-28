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
package com.flow.platform.api.service;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.dao.CredentialDao;
import com.flow.platform.api.domain.credential.AndroidCredentialDetail;
import com.flow.platform.api.domain.credential.Credential;
import com.flow.platform.api.domain.credential.CredentialDetail;
import com.flow.platform.api.domain.credential.CredentialType;
import com.flow.platform.api.domain.credential.IosCredentialDetail;
import com.flow.platform.api.domain.credential.RSACredentialDetail;
import com.flow.platform.api.domain.credential.RSAKeyPair;
import com.flow.platform.api.domain.credential.UsernameCredentialDetail;
import com.flow.platform.api.domain.file.FileResource;
import com.flow.platform.api.domain.file.PasswordFileResource;
import com.flow.platform.api.envs.GitEnvs;
import com.flow.platform.api.util.ZipUtil;
import com.flow.platform.core.exception.FlowException;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.util.CollectionUtil;
import com.flow.platform.util.ExceptionUtil;
import com.flow.platform.util.StringUtil;
import com.google.common.base.Strings;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.PostConstruct;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author lhl
 */

@Service
@Transactional
public class CredentialServiceImpl extends CurrentUser implements CredentialService {

    private final static String PRIVATE_KEY_SUFFIX = "id_rsa";

    private final static String PUBLIC_KEY_SUFFIX = "id_rsa.pub";

    private final static String ZIP_SUFFIX = ".zip";

    private final static String CREDENTIAL_FOLDER = "credentials";

    @Autowired
    private CredentialDao credentialDao;

    @Autowired
    private Path workspace;

    private final Map<CredentialType, DetailHandler> handlerMapping = new HashMap<>();

    @PostConstruct
    public void init() {
        handlerMapping.put(CredentialType.RSA, new RSADetailHandler());
        handlerMapping.put(CredentialType.USERNAME, new UsernameDetailHandler());
        handlerMapping.put(CredentialType.ANDROID, new AndroidDetailHandler());
        handlerMapping.put(CredentialType.IOS, new IosDetailHandler());
    }

    @Override
    public List<Credential> list(Collection<CredentialType> types) {
        if (CollectionUtil.isNullOrEmpty(types)) {
            return credentialDao.list();
        }

        return credentialDao.listByType(types);
    }

    @Override
    public Resource download(String name) {
        Credential credential = credentialDao.get(name);

        if (Objects.isNull(credential)) {
            throw new FlowException("Credential not found " + name);
        }

        Resource resource = handlerMapping.get(credential.getDetail().getType())
            .resource(credential.getDetail(), credential.getName());

        if (Objects.isNull(resource)) {
            throw new FlowException("Not found resource");
        }

        return resource;
    }

    @Override
    public Credential createOrUpdate(String name, CredentialDetail detail) {
        Credential credential = credentialDao.get(name);

        if (credential == null) {
            credential = new Credential(name);
        }

        // create xxCredentialDetailHandler instance by name
        handlerMapping.get(detail.getType()).handle(detail);

        credential.setType(detail.getType());
        credential.setDetail(detail);
        credential.setCreatedBy(currentUser().getEmail());
        credentialDao.saveOrUpdate(credential);

        return credentialDao.get(name);
    }

    @Override
    public boolean existed(String name) {
        return credentialDao.get(name) != null;
    }

    @Override
    public Map<String, String> findByName(String rsaOrUsernameCredentialName) {
        if (Strings.isNullOrEmpty(rsaOrUsernameCredentialName)) {
            return Collections.emptyMap();
        }

        try {
            Credential credential = find(rsaOrUsernameCredentialName);
            CredentialType credentialType = credential.getType();

            // for git ssh client needs rsa credential
            if (credentialType.equals(CredentialType.RSA)) {
                RSACredentialDetail credentialDetail = (RSACredentialDetail) credential.getDetail();

                Map<String, String> envs = new HashMap<>(2);
                envs.put(GitEnvs.FLOW_GIT_SSH_PRIVATE_KEY.name(), credentialDetail.getPrivateKey());
                envs.put(GitEnvs.FLOW_GIT_SSH_PUBLIC_KEY.name(), credentialDetail.getPublicKey());
                return envs;
            }

            // for git http client needs username credential
            if (credentialType.equals(CredentialType.USERNAME)) {

                UsernameCredentialDetail credentialDetail = (UsernameCredentialDetail) credential.getDetail();

                Map<String, String> envs = new HashMap<>(2);
                envs.put(GitEnvs.FLOW_GIT_HTTP_USER.name(), credentialDetail.getUsername());
                envs.put(GitEnvs.FLOW_GIT_HTTP_PASS.name(), credentialDetail.getPassword());
                return envs;
            }

        } catch (IllegalParameterException ignore) {
            // credential not found
        }

        return Collections.emptyMap();
    }

    @Override
    public Credential find(String name) {
        Credential credential = credentialDao.get(name);

        if (credential == null) {
            throw new IllegalParameterException("Credential '" + name + "' doesn't existed");
        }

        return credential;
    }

    @Override
    public void delete(String name) {
        Credential credential = find(name);
        credentialDao.delete(credential);
        deleteZipResource(name);
    }

    @Override
    public RSAKeyPair generateRsaKey() {
        String comment = "FLOWCI";
        int type = KeyPair.RSA;
        final int keySize = 2048; // default 1024, bitbucket support at least 2048
        JSch jsch = new JSch();

        try {
            KeyPair kpair = KeyPair.genKeyPair(jsch, type, keySize);
            RSAKeyPair pair = new RSAKeyPair();

            // private key
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                kpair.writePrivateKey(baos);
                pair.setPrivateKey(baos.toString());
            }

            // public key
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                kpair.writePublicKey(baos, comment);
                pair.setPublicKey(baos.toString());
            }

            kpair.dispose();
            return pair;

        } catch (JSchException | IOException e) {
            return null;
        }
    }

    private Path buildCredentialPath(String name) {
        Path credentialPath = Paths.get(workspace.toString(), CREDENTIAL_FOLDER, name);
        try {
            Files.createDirectories(credentialPath);
        } catch (IOException e) {
            throw new FlowException("Create tmp directory happens exceptions " + ExceptionUtil.findRootCause(e));
        }

        return credentialPath;
    }

    private void deleteZipResource(String name) {
        Path path = buildCredentialPath(name);
        try {
            Files.deleteIfExists(Paths.get(path.toString() + ZIP_SUFFIX));
        } catch (IOException e) {
        }
    }

    private abstract class DetailHandler<T extends CredentialDetail> {

        abstract void handle(T detail);

        public Resource resource(T detail, String name) {
            Path credentialPath = buildCredentialPath(name);
            Path zipPath = Paths.get(credentialPath.toString() + ZIP_SUFFIX);
            File targetFile = new File(zipPath.toString());
            Resource resource;

            if (!targetFile.exists()) {
                loadResource(detail, credentialPath);
            }

            try {
                InputStream inputStream = new FileInputStream(targetFile);
                resource = new InputStreamResource(inputStream);
            } catch (IOException e) {
                throw new FlowException("Io exception " + ExceptionUtil.findRootCause(e));
            } finally {
                if (Files.exists(credentialPath)) {
                    deleteResource(credentialPath);
                }
            }

            return resource;
        }

        public void loadResource(T detail, Path tmp) {
        }

        public void deleteResource(Path tmp) {
            try {
                FileUtils.deleteDirectory(tmp.toFile());
            } catch (IOException e) {
                throw new FlowException("Io exception " + ExceptionUtil.findRootCause(e));
            }
        }
    }

    private class RSADetailHandler extends DetailHandler<RSACredentialDetail> {

        @Override
        public void handle(RSACredentialDetail detail) {
            if (StringUtil.isNullOrEmptyForItems(detail.getPrivateKey(), detail.getPublicKey())) {
                RSAKeyPair pair = generateRsaKey();
                detail.setPublicKey(pair.getPublicKey());
                detail.setPrivateKey(pair.getPrivateKey());
            }
        }


        /**
         * read resource to zip file
         * @param detail
         * @param tmp
         */
        @Override
        public void loadResource(RSACredentialDetail detail, Path tmp) {
            Path zipPath = Paths.get(tmp.toString() + ZIP_SUFFIX);
            Path privateKey = Paths.get(tmp.toString(), PRIVATE_KEY_SUFFIX);
            Path publicKey = Paths.get(tmp.toString(), PUBLIC_KEY_SUFFIX);
            File targetFile = new File(zipPath.toString());
            try {
                Files.write(privateKey, detail.getPrivateKey().getBytes(AppConfig.DEFAULT_CHARSET));
                Files.write(publicKey, detail.getPublicKey().getBytes(AppConfig.DEFAULT_CHARSET));

                ZipUtil.zipFolder(tmp.toFile(), targetFile);

            } catch (IOException e) {
                throw new FlowException("Io exception " + ExceptionUtil.findRootCause(e));
            }
        }

    }

    private class UsernameDetailHandler extends DetailHandler<UsernameCredentialDetail> {

        @Override
        public void handle(UsernameCredentialDetail detail) {

        }
    }

    private class AndroidDetailHandler extends DetailHandler<AndroidCredentialDetail> {

        @Override
        public void handle(AndroidCredentialDetail detail) {

        }
    }

    private class IosDetailHandler extends DetailHandler<IosCredentialDetail> {

        @Override
        public void handle(IosCredentialDetail detail) {
        }

        @Override
        public void loadResource(IosCredentialDetail detail, Path tmp) {
            Path zipPath = Paths.get(tmp.toString() + ZIP_SUFFIX);
            File targetFile = new File(zipPath.toString());

            try {
                for (PasswordFileResource passwordFileResource : detail.getP12s()) {
                    FileUtils.copyFileToDirectory(Paths.get(passwordFileResource.getPath()).toFile(), tmp.toFile());
                }

                for (FileResource fileResource : detail.getProvisionProfiles()) {
                    FileUtils.copyFileToDirectory(Paths.get(fileResource.getPath()).toFile(), tmp.toFile());
                }

                ZipUtil.zipFolder(tmp.toFile(), targetFile);
            } catch (IOException e) {
                throw new FlowException("Io exception " + ExceptionUtil.findRootCause(e));
            }
        }
    }
}