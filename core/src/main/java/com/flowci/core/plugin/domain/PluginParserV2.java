package com.flowci.core.plugin.domain;

import com.flowci.common.domain.Input;
import com.flowci.common.domain.VarType;
import com.flowci.common.helper.YamlHelper;
import com.flowci.tree.yml.DockerYml;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.io.InputStream;
import java.util.List;

public abstract class PluginParserV2 {

    public static PluginV2 parse(InputStream is) {
        var yaml = YamlHelper.create(PluginV2Wrapper.class);
        var wrapper = yaml.<PluginV2Wrapper>load(is);
        return wrapper.toObject();
    }

    private static class PluginV2Wrapper {

        public String id;

        public String name;

        public String desc;

        public String icon;

        public String[] tags;

        public List<PluginV2.Author> authors;

        public List<VariableWrapper> inputs;

        public Boolean allow_failure;

        public String bash;

        public String pwsh;

        public String script;

        public DockerYml docker;

        public PluginV2 toObject() {

        }
    }

    @NoArgsConstructor
    private static class VariableWrapper {

        @NonNull
        public String name;

        public String alias;

        @NonNull
        public String type;

        @NonNull
        public Boolean required;

        // default value
        public String value;

        public Input toVariable() {
            Input var = new Input(name, VarType.valueOf(type.toUpperCase()));
            var.setRequired(required);
            var.setAlias(alias);
            var.setValue(value);
            return var;
        }
    }
}
