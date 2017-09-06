package com.divinitor.discord.wahrbot.core.module;

import com.github.zafarkhaja.semver.Version;
import lombok.Getter;

@Getter
public class ModuleInformation {


    private String name;

    private String mainClass;

    private String id;

    private Version version;

    public ModuleInformation() {
    }
}
