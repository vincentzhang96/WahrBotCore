package com.divinitor.discord.wahrbot.core.i18n;

import java.util.*;

public interface Localizer {

    String ERROR_PREFIX = "##!";
    String NO_SUCH_KEY = ERROR_PREFIX + "MISSING_KEY:";
    String UNSUPPORTED_LOCALE = ERROR_PREFIX + "UNSUPPORTED_LOCALE:";
    String BAD_ARG_INDEX = ERROR_PREFIX + "BAD_ARG_INDEX:";
    String BAD_ARG = ERROR_PREFIX + "BAD_ARG:";
    String BAD_NAMED_ARG = ERROR_PREFIX + "BAD_NAMED_ARG:";
    String BAD_FORMAT_STRING = ERROR_PREFIX + "BAD_FORMAT_STRING:";
    String NO_MATCHING_PLURAL = ERROR_PREFIX + "NO_MATCHING_PLURAL:";
    String BAD_REFERENCE = ERROR_PREFIX + "BAD_REFERENCE:";

    String PREFIX_DO_NOT_RESOLVE = "!";

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

    /**
     * Register the provided plurality rules to this Localizer
     * @param rules A map of plurality rules. key: name, value: LocalizerPluralRule
     */
    void registerPluralityRules(Map<String, LocalizerPluralRule> rules);

    Map<String, LocalizerPluralRule> getPluralRules();

    /**
     * Returns a map of default plurality rules that can be added via {@link Localizer#registerPluralityRules(Map)}
     * <br/>
     * The default rules are:
     * <ul>
     *     <li> ZERO - (int) n == 0</li>
     *     <li> ZERO_F - (double) n < {@link #EPSILON}</li>
     *     <li> ONE - (int) n == 1</li>
     *     <li> ONE_F - |(double) n - 1| < {@link #EPSILON}</li>
     *     <li> MANY - n > 1</li>
     *     <li> EN_PLURAL - n != 1 (English plural)</li>
     * </ul>
     */
    static Map<String, LocalizerPluralRule> defaultPluralityRules() {
        Map<String, LocalizerPluralRule> defaultRules = new HashMap<>();
        defaultRules.put("ZERO", n -> n.intValue() == 0);
        defaultRules.put("ZERO_F", n -> n.doubleValue() < EPSILON);
        defaultRules.put("ONE", n -> n.intValue() == 1);
        LocalizerPluralRule oneFloat = n -> Math.abs(n.doubleValue() - 1D) < EPSILON;
        defaultRules.put("ONE_F", oneFloat);
        defaultRules.put("MANY", n -> n.doubleValue() > 1D);
        defaultRules.put("EN_PLURAL", oneFloat.negate());
        return defaultRules;
    }

    double EPSILON = 0.00000001D;
}
