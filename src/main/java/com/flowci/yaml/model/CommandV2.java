package com.flowci.yaml.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import static org.springframework.util.StringUtils.hasText;

@Getter
@Setter
public class CommandV2 {

    private String name; // optional

    private String bash; // bash script

    private String pwsh; // powershell script

    public String getBash() {
        return hasText(bash) ? bash.trim() : null;
    }

    public String getPwsh() {
        return hasText(pwsh) ? pwsh.trim() : null;
    }
}
