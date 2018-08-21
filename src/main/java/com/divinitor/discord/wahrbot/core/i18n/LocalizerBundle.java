package com.divinitor.discord.wahrbot.core.i18n;

import java.util.Locale;
import java.util.stream.Stream;

public interface LocalizerBundle {

    String get(String key, Locale locale);

    boolean contains(String key, Locale locale);

    default Stream<String> keys(Locale locale) {
        return Stream.empty();
    }
}
