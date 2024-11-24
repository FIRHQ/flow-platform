package com.flowci.core.plugin.domain;

import com.flowci.common.domain.DockerOption;
import com.flowci.common.domain.Input;
import com.flowci.common.domain.Version;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@Document(collection = "plugins_v2")
@ToString(of = "name")
public class PluginV2 {

    private String id;

    private String name;

    // from git repo tag
    private List<Version> versions;

    private String desc;

    private String icon;

    private String[] tags;

    private List<Author> authors;

    private List<Input> inputs = new LinkedList<>();

    private Set<String> exports = new HashSet<>();

    private boolean allowFailure;

    private DockerOption docker;

    private String bash;

    private String pwsh;

    private Instant syncTime;

    // directory in workspace
    private String dir;

    @Getter
    @Setter
    public static class Author {

        private String name;

        private String email;
    }
}
