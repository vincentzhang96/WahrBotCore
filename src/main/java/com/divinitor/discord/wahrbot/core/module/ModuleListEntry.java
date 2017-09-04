package com.divinitor.discord.wahrbot.core.module;

import com.github.zafarkhaja.semver.Version;
import lombok.Data;

@Data
public class ModuleListEntry {

    private String id;
    private Version version;

}
