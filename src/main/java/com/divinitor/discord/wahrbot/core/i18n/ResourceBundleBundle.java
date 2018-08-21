package com.divinitor.discord.wahrbot.core.i18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ResourceBundleBundle implements LocalizerBundle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String bundleLocation;
    private WeakReference<ClassLoader> classloader;

    public ResourceBundleBundle(String bundle) {
        this(bundle, null);
    }

    public ResourceBundleBundle(String bundle, ClassLoader classloader) {
        this.bundleLocation = bundle;
        this.classloader = new WeakReference<>(classloader);
    }

    @Override
    public String get(String key, Locale locale) {

        ResourceBundle bundle;
        try {
            bundle = getBundle(locale);
        } catch (MissingResourceException mre) {
            LOGGER.warn("Unable to load bundle at {}", this.bundleLocation);
            return null;
        }
        try {
            return bundle.getString(key);
        } catch (MissingResourceException mre) {
            //  Missing key is normal
//            LOGGER.warn("Missing key {} in bundle {}", key, this.bundleLocation);
            return null;
        }
    }

    private ResourceBundle getBundle(Locale locale) {
        ClassLoader loader = this.classloader.get();
        if (loader == null) {
            loader = this.getClass().getClassLoader();
        }

        return ResourceBundle.getBundle(this.bundleLocation, locale, loader);
    }

    @Override
    public boolean contains(String key, Locale locale) {
        try {
            return getBundle(locale).containsKey(key);
        } catch (MissingResourceException mre) {
            LOGGER.warn("Unable to load bundle at {}", this.bundleLocation);
            return false;
        }
    }

    @Override
    public Stream<String> keys(Locale locale) {
        ResourceBundle bundle;
        try {
            bundle = getBundle(locale);
        } catch (MissingResourceException mre) {
            LOGGER.warn("Unable to load bundle at {}", this.bundleLocation);
            return Stream.empty();
        }

        return bundle.keySet().stream();
    }
}
