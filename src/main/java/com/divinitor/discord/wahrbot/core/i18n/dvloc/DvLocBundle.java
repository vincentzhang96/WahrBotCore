package com.divinitor.discord.wahrbot.core.i18n.dvloc;

import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.i18n.LocalizerBundle;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.util.Locale;

public class DvLocBundle implements LocalizerBundle {

    private String prefix;

    private final Table<Locale, String, String> data;

    public DvLocBundle() {
        this.data = HashBasedTable.create();
    }

    @Override
    public String get(String key, Locale locale) {
        if (key.startsWith(prefix)) {
            key = key.substring(prefix.length());
            String ret = this.data.get(locale, key);
            return ret;
        } else {
            return null;
        }
    }

    @Override
    public boolean contains(String key, Locale locale) {
        if (key.startsWith(prefix)) {
            key = key.substring(prefix.length());
            return this.data.contains(locale, key);
        } else {
            return false;
        }
    }
}
