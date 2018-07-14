package com.divinitor.discord.wahrbot.core.util.gson;

import com.divinitor.discord.wahrbot.core.util.gson.adapters.VersionTypeAdapter;
import com.github.zafarkhaja.semver.Version;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Provides static singleton instances of standard Gson configurations.
 */
public class StandardGson {

    /**
     * Compact Gson instance
     */
    private static final Gson INSTANCE;

    /**
     * Pretty printing Gson instance
     */
    private static final Gson PRETTY_INSTANCE;

    static {
        INSTANCE = new GsonBuilder()
            .registerTypeAdapter(Version.class, new VersionTypeAdapter())
            .create();

        PRETTY_INSTANCE = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Version.class, new VersionTypeAdapter())
            .create();
    }

    /**
     * Get the compact form Gson instance
     * @return A Gson instance that outputs compact JSON
     */
    public static Gson instance() {
        return INSTANCE;
    }

    /**
     * Get the pretty printing Gson instance
     * @return A Gson instance that pretty prints
     */
    public static Gson pretty() {
        return PRETTY_INSTANCE;
    }

    /**
     * Private constructor
     */
    private StandardGson() {
    }
}
