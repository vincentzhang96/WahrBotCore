package com.divinitor.discord.wahrbot.core.module;

import com.divinitor.discord.wahrbot.core.util.progress.ProgressListener;

import java.util.Map;

public interface ModuleManager {

    void unloadModule(String moduleId, ProgressListener listener);

    void reloadModule(String moduleId, ProgressListener listener);

    void loadModule(String moduleId, ProgressListener listener);

    Map<String, ModuleHandle> getLoadedModules();
}
