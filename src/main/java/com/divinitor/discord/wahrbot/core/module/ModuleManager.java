package com.divinitor.discord.wahrbot.core.module;

import com.github.zafarkhaja.semver.Version;

import java.util.Map;

public interface ModuleManager {

    void unloadModule(String moduleId);

    void reloadModule(String moduleId, Version newVersion);

    void loadModule(String moduleId, Version version);

    void loadModulesFromList();

    /**
     * Returns a map of all modules that are loaded. The returned map is unmodifiable and contains interally weak
     * references to the actual module handles and modules. This collection is safe to keep around and will not
     * prevent modules from being unloaded. However, storing Modules returned by the contained ModuleHandles is
     * <b>NOT</b> safe and should be kept only locally or in a WeakReference. If a module contained in this result
     * is unloaded, the corresponding {@link ModuleHandle}'s methods will throw an IllegalStateException.
     *
     * @return A Map of loaded modules, mapping module IDs to ModuleHandles.
     */
    Map<String, ModuleHandle> getLoadedModules();
}
