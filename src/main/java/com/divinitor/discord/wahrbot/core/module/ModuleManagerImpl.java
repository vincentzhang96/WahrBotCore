package com.divinitor.discord.wahrbot.core.module;

import com.divinitor.discord.wahrbot.core.WahrBotImpl;
import com.divinitor.discord.wahrbot.core.util.concurrent.Lockable;
import com.divinitor.discord.wahrbot.core.util.gson.StandardGson;
import com.github.zafarkhaja.semver.Version;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import static com.divinitor.discord.wahrbot.core.util.concurrent.Lockable.acquire;

public class ModuleManagerImpl implements ModuleManager {

    private final Map<String, ModuleHandleImpl> loadedModules;
    private final ReentrantReadWriteLock lock;
    private final WahrBotImpl bot;

    @Inject
    public ModuleManagerImpl(WahrBotImpl bot) {
        this.bot = bot;
        this.loadedModules = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }


    @Override
    public void unloadModule(String moduleId) {
        try (Lockable l = acquire(this.lock.writeLock())) {
            ModuleHandleImpl moduleHandle = this.loadedModules.remove(moduleId);
            if (moduleHandle == null) {
                throw new NoSuchElementException(moduleId);
            }

            try {
                moduleHandle.getModule().shutDown();
            } catch (Exception e) {
                //  TODO
            }
        }
    }

    @Override
    public void reloadModule(String moduleId, Version newVersion) {
        //  Acquire the lock once to keep things atomic
        try (Lockable l = acquire(this.lock.writeLock())) {
            this.unloadModule(moduleId);
            this.loadModule(moduleId, newVersion);
        }
    }

    @Override
    public void loadModule(String moduleId, Version version) {
        try (Lockable l = acquire(this.lock.writeLock())) {
            if (this.loadedModules.containsKey(moduleId)) {
                throw new IllegalStateException("Module " + moduleId + " is already loaded. Unload it first.");
            }

            Path dir = this.bot.getBotDir().resolve("module").resolve(moduleId).resolve(version.toString());
            Path jar = dir.resolve(String.format("%s-%s.jar", moduleId, version.toString()));

            if (!Files.isRegularFile(jar)) {
                
            }

            JarFile jarFile = new JarFile(jar.toFile());
            ZipEntry entry = jarFile.getEntry("moduleinfo.json");

            InputStream inputStream = jarFile.getInputStream(entry);
            ModuleInformation info = StandardGson.instance().fromJson(
                    new InputStreamReader(inputStream),
                    ModuleInformation.class);

            //  Verify
            if (!moduleId.equals(info.getId())) {
                throw new IllegalArgumentException("Module IDs do not match: Expected " + moduleId +
                        ", got " + info.getId());
            }

            if (!version.equals(info.getVersion())) {
                throw new IllegalArgumentException("Module versions do not match: Expected " + version +
                        ", got " + info.getVersion());
            }

            //  Attempt to classload
            ModuleClassLoader loader = new ModuleClassLoader(jar, this.getClass().getClassLoader());
            Module module = this.bot.getInjector().getInstance(loader.findClass(info.getMainClass()));

            ModuleHandleImpl handle = new ModuleHandleImpl(module, loader, info);
            this.loadedModules.put(moduleId, handle);

            try {
                module.init(handle);
            } catch (Exception e) {
                //  TODO

            }
        }
    }

    @Override
    public void loadModulesFromList() {
        Path moduleList = this.bot.getBotDir().resolve("module").resolve("modules.json");
        try (BufferedReader bufferedReader = Files.newBufferedReader(moduleList, StandardCharsets.UTF_8)) {
            Map<String, String> values = StandardGson.pretty().fromJson(bufferedReader,
                    new TypeToken<Map<String, String>>() {}.getType());

            try (Lockable l = acquire(lock.writeLock())) {
                values.forEach((k, v) -> this.loadModule(k, Version.valueOf(v)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, ModuleHandle> getLoadedModules() {
        try (Lockable l = acquire(this.lock.readLock())) {
            Map<String, WeakModuleHandleImpl> ret = new HashMap<>(
                    this.loadedModules.values()
                    .stream()
                    .map(WeakModuleHandleImpl::new)
                    .collect(Collectors.toMap((ModuleHandle h) -> h.getModuleInfo().getId(), Function.identity()))
            );
            return Collections.unmodifiableMap(ret);
        }
    }

    static class ModuleHandleImpl implements ModuleHandle, ModuleContext {

        private final Module module;
        private final ModuleClassLoader classLoader;
        private final ModuleInformation moduleInfo;

        ModuleHandleImpl(Module module, ModuleClassLoader classLoader, ModuleInformation moduleInfo) {
            this.module = module;
            this.classLoader = classLoader;
            this.moduleInfo = moduleInfo;
        }

        @Override
        public Module getModule() {
            return this.module;
        }

        @Override
        public ModuleInformation getModuleInfo() {
            return this.moduleInfo;
        }
    }

    static class WeakModuleHandleImpl implements ModuleHandle {
        private final WeakReference<ModuleHandleImpl> handle;

        WeakModuleHandleImpl(ModuleHandleImpl handle) {
            this.handle = new WeakReference<>(handle);
        }

        private ModuleHandleImpl tryGetHandle() {
            ModuleHandleImpl moduleHandle = this.handle.get();
            if (moduleHandle == null) {
                throw new IllegalStateException("Module has been unloaded already");
            }
            return moduleHandle;
        }

        @Override
        public Module getModule() {
            return this.tryGetHandle().getModule();
        }

        @Override
        public ModuleInformation getModuleInfo() {
            return this.tryGetHandle().getModuleInfo();
        }
    }
}
