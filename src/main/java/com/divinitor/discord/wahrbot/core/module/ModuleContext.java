package com.divinitor.discord.wahrbot.core.module;

import com.divinitor.discord.wahrbot.core.WahrBot;

public interface ModuleContext {

    WahrBot getBot();

    ModuleInformation getModuleInfo();

}
