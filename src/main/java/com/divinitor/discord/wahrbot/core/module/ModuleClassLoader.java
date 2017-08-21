package com.divinitor.discord.wahrbot.core.module;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ModuleClassLoader extends URLClassLoader {

    private final Path jar;
    private final Map<String, Class<?>> classes;
    private final Set<String> parentClasses;

    public ModuleClassLoader(Path jar, ClassLoader parent) {
        super(pathToUrls(jar), parent);
        this.jar = jar;
        this.classes = new HashMap<>();
        this.parentClasses = new HashSet<>();
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> clazz = classes.get(name);
        if (clazz != null) {
            return clazz;
        }

        if (parentClasses.contains(name)) {
            throw new ClassNotFoundException(name);
        }

        clazz = super.findClass(name);
        if (clazz == null) {
            parentClasses.add(name);
            throw new ClassNotFoundException(name);
        }

        classes.put(name, clazz);
        return clazz;
    }

    private static URL[] pathToUrls(Path path) {
        try {
            return new URL[] { path.toUri().toURL() };
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
