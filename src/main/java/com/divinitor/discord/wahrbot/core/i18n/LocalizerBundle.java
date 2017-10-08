package com.divinitor.discord.wahrbot.core.i18n;

import java.util.Locale;

public interface LocalizerBundle {

    String get(String key, Locale locale);

    boolean contains(String key, Locale locale);

}
