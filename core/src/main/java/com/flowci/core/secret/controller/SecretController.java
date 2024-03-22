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

package com.flowci.core.secret.controller;

import com.flowci.common.domain.SimpleKeyPair;
import com.flowci.core.auth.annotation.Action;
import com.flowci.core.secret.domain.AndroidSignOption;
import com.flowci.core.secret.domain.Request;
import com.flowci.core.secret.domain.Secret;
import com.flowci.core.secret.service.SecretService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author yang
 */
@RestController
@RequestMapping("/secrets")
public class SecretController {

    @Autowired
    private SecretService secretService;

    @GetMapping("/{name}")
    @Action(SecretActions.GET)
    public Secret getByName(@PathVariable String name) {
        return secretService.get(name);
    }

    @GetMapping
    @Action(SecretActions.LIST)
    public List<Secret> list() {
        return secretService.list();
    }

    @GetMapping("/list/name")
    @Action(SecretActions.LIST_NAME)
    public List<Secret> listName(@RequestParam String category) {
        return secretService.listName(category);
    }

    @PostMapping("/rsa")
    @Action(SecretActions.CREATE)
    public Secret create(@Validated @RequestBody Request.CreateRSA body) {
        if (body.hasKeyPair()) {
            return secretService.createRSA(body.getName(), body.getKeyPair());
        }

        return secretService.createRSA(body.getName());
    }

    @PostMapping("/auth")
    @Action(SecretActions.CREATE)
    public Secret create(@Validated @RequestBody Request.CreateAuth body) {
        return secretService.createAuth(body.getName(), body.getAuthPair());
    }

    @PostMapping("/token")
    @Action(SecretActions.CREATE)
    public Secret create(@Validated @RequestBody Request.CreateToken body) {
        return secretService.createToken(body.getName(), body.getToken());
    }

    @PostMapping("/android/sign")
    @Action(SecretActions.CREATE)
    public Secret create(@Validated @NotEmpty @RequestPart String name,
                         @Validated @RequestPart AndroidSignOption option,
                         @Validated @NotBlank @RequestPart MultipartFile keyStore) {

        return secretService.createAndroidSign(name, keyStore, option);
    }

    @PostMapping("/kubeconfig")
    @Action(SecretActions.CREATE)
    public Secret create(@Validated @RequestBody Request.CreateKubeConfig body) {
        return secretService.createKubeConfig(body.getName(), body.getContent());
    }

    @PostMapping("/rsa/gen")
    @Action(SecretActions.GENERATE_RSA)
    public SimpleKeyPair genByEmail() {
        return secretService.genRSA();
    }

    @DeleteMapping("/{name}")
    @Action(SecretActions.DELETE)
    public Secret delete(@PathVariable String name) {
        return secretService.delete(name);
    }
}
