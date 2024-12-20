package com.flowci.yaml.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.List;

@Getter
@Setter
public class DockerV2 {

    private String image;

    private String auth; // auth secret for private docker registry

    private String name;

    private String network;

    private List<String> ports;

    private List<String> entrypoint;

    private List<String> command;

    private Map<String, String> environment;

    @JsonProperty("is_runtime")
    private Boolean isRuntime;

    @JsonProperty("stop_on_finish")
    private Boolean stopOnFinish;

    @JsonProperty("delete_on_finish")
    private Boolean deleteOnFinish;
}
