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

package com.flow.platform.plugin.test.util;

import com.flow.platform.plugin.util.UriUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author gyfirim
 */
public class UriUtilTest {

    @Test
    public void should_get_route_path_success() {
        String url = "https://github.com/yunheli/info";
        Assert.assertEquals("/yunheli/info", UriUtil.getRoutePath(url));

        url = "https://github.com/yunheli/info?params=1";
        Assert.assertEquals("/yunheli/info", UriUtil.getRoutePath(url));
    }

    @Test
    public void should_detect_github_source_success() {
        String url = "https://github.com/yunheli/info";
        Assert.assertEquals(true, UriUtil.isGithubSource(url));

        url = "https://bitbucket.com/yunheli/info";
        Assert.assertEquals(false, UriUtil.isGithubSource(url));
    }
}
