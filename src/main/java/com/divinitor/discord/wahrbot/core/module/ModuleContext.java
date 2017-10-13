package com.divinitor.discord.wahrbot.core.module;

import com.google.inject.Injector;

public interface ModuleContext {

    ModuleInformation getModuleInfo();

    Injector getInjector();
}
