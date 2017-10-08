package com.divinitor.discord.wahrbot.core.i18n;

import java.util.Locale;

public interface Localizer {

    String ERROR_PREFIX = "##!";
    String NO_SUCH_KEY = ERROR_PREFIX + "MISSING_KEY:";
    String UNSUPPORTED_LOCALE = ERROR_PREFIX + "UNSUPPORTED_LOCALE:";
    String BAD_ARG_INDEX = ERROR_PREFIX + "BAD_ARG_INDEX:";
    String BAD_ARG = ERROR_PREFIX + "BAD_ARG:";
    String BAD_NAMED_ARG = ERROR_PREFIX + "BAD_NAMED_ARG:";

    /**
     * Localizes the given key in the default locale with no arguments.
     *
     * @param key The locale string key
     * @return The localized string, or an error code string
     * @see #localize(String, Object...)
     */
    default String localize(String key) {
        return localize(key, this.getDefaultLocale());
    }

    /**
     * Localizes the given key in the default locale, with the provided arguments for formatting.
     *
     * @param key  The locale string key
     * @param args The locale to use
     * @return The localized and formatted string, or an error code string
     * @see #localizeToLocale(String, Locale, Object...)
     */
    default String localize(String key, Object... args) {
        return this.localizeToLocale(key, this.getDefaultLocale(), args);
    }

    /**
     * <p>
     * Localizes the given key in the given locale, with the provided arguments for formatting.
     * </p>
     * <p>
     * If there is no match in the provided locale, the default locale is used.
     * </p>
     * <p>
     * If the last argument is a Map, then it is used for named parameter lookups. The map values may be a mix of
     * String and Provider<String> objects. Other types will be ignored and treated as not present.
     * </p>
     *
     * @param key    The locale string key
     * @param locale The locale to use
     * @param args   Zero or more arguments, for string formatting. If the last argument is a Map, then it is used
     *               for parameter lookups
     * @return The localized and formatted string, or an error code string
     */
    String localizeToLocale(String key, Locale locale, Object... args);

    /**
     * Gets the default locale for this localizer.
     *
     * @return The default locale
     */
    Locale getDefaultLocale();

    /**
     * Whether or not the given locale key exists
     *
     * @param key The locale string key
     * @return True if it exists, false otherwise
     */
    boolean contains(String key);

    /**
     * Sets the default locale.
     *
     * @param locale The new default locale
     */
    void setDefaultLocale(Locale locale);

    /**
     * Adds a localization bundle to the registry.
     *
     * @param bundleKey The key used to identify the bundle
     * @param bundle The bundle
     */
    void registerBundle(String bundleKey, LocalizerBundle bundle);

    /**
     * Unregisters a localization bundle from the registry.
     *
     * @param bundleKey The bundle to unregister
     */
    void unregisterBundle(String bundleKey);

}
