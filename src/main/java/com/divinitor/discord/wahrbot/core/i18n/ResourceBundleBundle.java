package com.divinitor.discord.wahrbot.core.i18n;

import java.lang.ref.WeakReference;
import java.util.*;

public class ResourceBundleBundle implements LocalizerBundle {

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
        ResourceBundle bundle = getBundle(locale);

        try {
            return bundle.getString(key);
        } catch (MissingResourceException mre) {
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
        return getBundle(locale).containsKey(key);
    }
}
