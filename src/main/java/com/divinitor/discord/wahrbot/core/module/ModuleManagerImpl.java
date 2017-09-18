package com.divinitor.discord.wahrbot.core.module;

import com.divinitor.discord.wahrbot.core.WahrBotImpl;
import com.divinitor.discord.wahrbot.core.util.concurrent.Lockable;
import com.divinitor.discord.wahrbot.core.util.gson.StandardGson;
import com.github.zafarkhaja.semver.Version;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import static com.divinitor.discord.wahrbot.core.util.concurrent.Lockable.acquire;

public class ModuleManagerImpl implements ModuleManager {

    private final Map<String, ModuleHandleImpl> loadedModules;
    private final ReentrantReadWriteLock lock;
    private final WahrBotImpl bot;
    private final Path modDir;

    private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public ModuleManagerImpl(WahrBotImpl bot) {
        this.bot = bot;
        this.loadedModules = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.modDir = this.bot.getBotDir().resolve("module");
    }

    @Override
    public void unloadModule(String moduleId) {
        unloadModule(moduleId, false);
    }

    private boolean unloadModule(String moduleId, boolean reload) {
        moduleId = moduleId.toLowerCase();
        try (Lockable l = acquire(this.lock.writeLock())) {
            ModuleHandleImpl moduleHandle = this.loadedModules.remove(moduleId);
            if (moduleHandle == null) {
                throw new NoSuchElementException(moduleId);
            }

            if (reload && !moduleHandle.getModuleInfo().isReloadable()) {
                this.loadedModules.put(moduleId, moduleHandle);
                LOGGER.warn("Attempted to unload {} which cannot be unloaded", moduleId);
                return false;
            }

            try {
                moduleHandle.getModule().shutDown();
            } catch (Exception e) {
                LOGGER.warn("Uncaught exception while unloading module {}",
                    moduleHandle.getModuleInfo().getIdAndVersion(),
                    e);
            }

            this.tryUnregister(moduleHandle.getModule());
        }

        return true;
    }

    @Override
    public void reloadModule(String moduleId, Version newVersion) throws ModuleLoadException {
        moduleId = moduleId.toLowerCase();
        if (newVersion == null) {
            this.reloadModule(moduleId);
            return;
        }

        //  Acquire the lock once to keep things atomic
        try (Lockable l = acquire(this.lock.writeLock())) {
            LOGGER.info("Reloading {} to v{}", moduleId, newVersion);
            if (this.unloadModule(moduleId, true)) {
                this.loadModule(moduleId, newVersion);
            }
        }
    }

    @Override
    public void reloadModule(String moduleId) throws ModuleLoadException {
        moduleId = moduleId.toLowerCase();
        //  Acquire the lock once to keep things atomic
        try (Lockable l = acquire(this.lock.writeLock())) {
            LOGGER.info("Reloading {} to latest", moduleId);
            if (this.unloadModule(moduleId, true)) {
                this.loadModule(moduleId);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void loadModule(String moduleId, Version version) throws ModuleLoadException {
        moduleId = moduleId.toLowerCase();
        if (version == null) {
            this.loadModule(moduleId);
            return;
        }

        try (Lockable l = acquire(this.lock.writeLock())) {
            if (this.loadedModules.containsKey(moduleId)) {
                throw new IllegalStateException("Module " + moduleId + " is already loaded. Unload it first.");
            }

            String modIdAndVer = moduleId + ":" + version.toString();

            Path dir = this.modDir.resolve(moduleId);
            Path jar = dir.resolve(String.format("%s-%s.jar", moduleId, version.toString()));

            if (!Files.isRegularFile(jar)) {
                throw new ModuleLoadException("Jar for module " + modIdAndVer + " does not exist",
                    new FileNotFoundException(jar.toString()));
            }

            ModuleInformation info;
            try {
                JarFile jarFile = new JarFile(jar.toFile());
                ZipEntry entry = jarFile.getEntry("moduleinfo.json");

                InputStream inputStream = jarFile.getInputStream(entry);
                info = StandardGson.instance().fromJson(
                    new InputStreamReader(inputStream),
                    ModuleInformation.class);
            } catch (IOException e) {
                throw new ModuleLoadException("Failed to read module " + modIdAndVer, e);
            }

            //  Verify
            if (!moduleId.equals(info.getId())) {
                throw new ModuleLoadException("Module IDs for " + modIdAndVer + " do not match: Expected " + moduleId +
                        ", got " + info.getId());
            }

            if (!version.equals(info.getVersion())) {
                throw new ModuleLoadException("Module versions for " + modIdAndVer + " do not match: Expected " +
                    version + ", got " + info.getVersion());
            }

            //  Attempt to classload
            ModuleClassLoader loader = new ModuleClassLoader(jar, this.getClass().getClassLoader());

            Module module;
            try {
                module = this.bot.getInjector().getInstance((Class<Module>) loader.findClass(info.getMainClass()));
            } catch (ClassNotFoundException e) {
                throw new ModuleLoadException("Invalid module " + modIdAndVer + ": Missing module main class "+
                    info.getMainClass());
            }

            ModuleHandleImpl handle = new ModuleHandleImpl(module, loader, info);
            this.loadedModules.put(moduleId, handle);

            try {
                module.init(handle);
                this.bot.getEventBus().register(module);
            } catch (ModuleLoadException mle) {
                throw mle;
            } catch (Exception e) {
                throw new ModuleLoadException("Uncaught exception while initializing module " + modIdAndVer, e);
            }

            LOGGER.info("Loaded module {}", info.getIdAndVersion());
        }
    }

    @Override
    public void loadModule(String modId) throws ModuleLoadException {
        String moduleId = modId.toLowerCase();
        Path dir = this.modDir.resolve(moduleId);

        if (!Files.isDirectory(dir)) {
            throw new ModuleLoadException("No such module " + moduleId);
        }

        Version latest;
        //  Find latest version
        try (Stream<Path> stream = Files.walk(dir, 1)) {
            latest = stream
                .filter(Files::isRegularFile)
                .filter((d) -> !dir.equals(d))
                .map(Path::getFileName)
                .map(Path::toString)
                .map(s -> s.substring(moduleId.length() + 1).replace(".jar", ""))
                .map(Version::valueOf)
                .sorted(Comparator.reverseOrder())
                .findFirst()
                .orElseThrow(() -> new ModuleLoadException("No such module " + moduleId));
        } catch (IOException e) {
            throw new ModuleLoadException("Unable to determine latest version for module " + moduleId, e);
        }

        LOGGER.info("Discovered module {}:{}", moduleId, latest);

        loadModule(moduleId, latest);
    }

    @Override
    public void unloadAll() {
        try (Lockable l = acquire(this.lock.writeLock())) {
            //  Perform a copy since can't modify loadedModules while iterating, so iterate on copy
            HashSet<String> loaded = new HashSet<>(this.loadedModules.keySet());
            loaded.forEach(this::unloadModule);
        }
    }

    @Override
    public void loadLatestModulesFromList() throws ModuleLoadException {
        Path moduleList = this.modDir.resolve("modules.json");
        final AtomicReference<ModuleLoadException> aggregate = new AtomicReference<>();
        try (BufferedReader bufferedReader = Files.newBufferedReader(moduleList, StandardCharsets.UTF_8)) {
            Map<String, String> values = StandardGson.pretty().fromJson(bufferedReader,
                new TypeToken<Map<String, String>>() {
                }.getType());

            try (Lockable l = acquire(lock.writeLock())) {
                values.forEach((k, v) -> {
                    try {
                        this.loadModule(k, Version.valueOf(v));
                    } catch (ModuleLoadException mle) {
                        if (aggregate.get() == null) {
                            aggregate.set(mle);
                        } else {
                            aggregate.get().addSuppressed(mle);
                        }
                    } catch (Exception e) {
                        if (aggregate.get() == null) {
                            ModuleLoadException mle = new ModuleLoadException(e);
                            aggregate.set(mle);
                        } else {
                            aggregate.get().addSuppressed(e);
                        }
                    }
                });
            }
        } catch (FileNotFoundException fnfe) {
            throw new ModuleLoadException("Could not load module list");
        } catch (IOException e) {
            if (aggregate.get() == null) {
                ModuleLoadException mle = new ModuleLoadException(e);
                aggregate.set(mle);
            } else {
                aggregate.get().addSuppressed(e);
            }
        }

        ModuleLoadException mle = aggregate.get();
        if (mle != null) {
            throw mle;
        }

        LOGGER.info("Loaded {} modules", this.loadedModules.size());
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

    private void tryUnregister(Object o) {
        try {
            this.bot.getEventBus().unregister(o);
        } catch (IllegalArgumentException ignored) {
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
