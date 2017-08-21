package com.divinitor.discord.wahrbot.core.module;

import com.github.zafarkhaja.semver.Version;

public class ModuleInformation {

    private String name;

    private String mainClass;

    private Version version;

    public ModuleInformation() {
    }

    public String getName() {
        return name;
    }

    public String getMainClass() {
        return mainClass;
    }

    public Version getVersion() {
        return version;
    }
}
