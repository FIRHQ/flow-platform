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

package com.flowci.core.test.secret;

import com.flowci.core.secret.domain.*;
import com.flowci.core.secret.service.SecretService;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.SimpleAuthPair;
import org.assertj.core.util.Strings;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

/**
 * @author yang
 */
public class SecretServiceTest extends SpringScenario {

    @Autowired
    private SecretService secretService;

    @Before
    public void login() {
        mockLogin();
    }

    @Test
    public void should_create_rsa_secret() {
        Secret rsa = secretService.createRSA("hello.rsa");
        Assert.assertNotNull(rsa);
        shouldHasCreatedAtAndCreatedBy(rsa);

        Assert.assertEquals(Secret.Category.SSH_RSA, rsa.getCategory());
        Assert.assertEquals(sessionManager.getUserEmail(), rsa.getCreatedBy());
        Assert.assertNotNull(rsa.getCreatedAt());
        Assert.assertNotNull(rsa.getUpdatedAt());

        Secret loaded = secretService.get("hello.rsa");
        Assert.assertTrue(loaded instanceof RSASecret);

        RSASecret secret = (RSASecret) loaded;
        Assert.assertFalse(Strings.isNullOrEmpty(secret.getPublicKey()));
        Assert.assertFalse(Strings.isNullOrEmpty(secret.getPrivateKey()));
        Assert.assertNotNull(secret.getMd5Fingerprint());
    }

    @Test
    public void should_create_auth_secret() {
        SimpleAuthPair sa = new SimpleAuthPair();
        sa.setUsername("test@flow.ci");
        sa.setPassword("12345");

        Secret auth = secretService.createAuth("hello.auth", sa);
        Assert.assertNotNull(auth);
        Assert.assertEquals(Secret.Category.AUTH, auth.getCategory());
        Assert.assertEquals(sessionManager.getUserEmail(), auth.getCreatedBy());
        Assert.assertNotNull(auth.getCreatedAt());
        Assert.assertNotNull(auth.getUpdatedAt());

        Secret loaded = secretService.get("hello.auth");
        Assert.assertTrue(loaded instanceof AuthSecret);

        AuthSecret keyPair = (AuthSecret) loaded;
        Assert.assertFalse(Strings.isNullOrEmpty(keyPair.getUsername()));
        Assert.assertFalse(Strings.isNullOrEmpty(keyPair.getPassword()));
    }

    @Test
    public void should_list_secret() {
        secretService.createRSA("hello.rsa.1");
        secretService.createRSA("hello.rsa.2");

        secretService.createAuth("hello.auth.1", SimpleAuthPair.of("111", "111"));
        secretService.createAuth("hello.auth.2", SimpleAuthPair.of("111", "111"));

        List<Secret> list = secretService.list();
        Assert.assertEquals(4, list.size());

        Assert.assertEquals("hello.rsa.1", list.get(0).getName());
        Assert.assertEquals("hello.rsa.2", list.get(1).getName());
        Assert.assertEquals("hello.auth.1", list.get(2).getName());
        Assert.assertEquals("hello.auth.2", list.get(3).getName());

        List<Secret> names = secretService.listName(null);
        Assert.assertEquals(4, names.size());

        Assert.assertEquals("hello.rsa.1", names.get(0).getName());
        Assert.assertEquals("hello.rsa.2", names.get(1).getName());
        Assert.assertEquals("hello.auth.1", list.get(2).getName());
        Assert.assertEquals("hello.auth.2", list.get(3).getName());
    }

    @Test
    public void should_create_android_sign_secret() {
        AndroidSignOption option = new AndroidSignOption();
        option.setKeyStorePassword("12345");
        option.setKeyAlias("helloworld");
        option.setKeyPassword("678910");

        MockMultipartFile ks = new MockMultipartFile("ks", "test.jks", null, "test data".getBytes());

        AndroidSign config = secretService.createAndroidSign("android-debug", ks, option);
        Assert.assertEquals("test.jks", config.getKeyStoreFileName());
        Assert.assertEquals("12345", config.getKeyStorePassword().getData());

        Assert.assertEquals("helloworld", config.getKeyAlias());
        Assert.assertEquals("678910", config.getKeyPassword().getData());
    }
}
