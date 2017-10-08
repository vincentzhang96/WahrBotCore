package com.divinitor.discord.wahrbot.core.i18n;

import java.util.*;

public class LocalizerImpl implements Localizer {

    private Locale defaultLocale;
    private Map<String, LocalizerBundle> bundles;

    public LocalizerImpl() {
        this(Locale.getDefault());
    }

    public LocalizerImpl(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
        this.bundles = new HashMap<>();
    }

    @Override
    public String localizeToLocale(String key, Locale locale, Object... args) {
        return null;
    }

    @Override
    public Locale getDefaultLocale() {
        return null;
    }

    @Override
    public boolean contains(String key) {
        return false;
    }

    @Override
    public void setDefaultLocale(Locale locale) {
        this.defaultLocale = locale;
    }

    @Override
    public void registerBundle(String bundleKey, LocalizerBundle bundle) {

    }

    @Override
    public void unregisterBundle(String bundleKey) {

    }
}
