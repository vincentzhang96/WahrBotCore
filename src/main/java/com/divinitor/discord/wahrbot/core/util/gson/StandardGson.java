package com.divinitor.discord.wahrbot.core.util.gson;

import com.divinitor.discord.wahrbot.core.util.gson.adapters.VersionTypeAdapter;
import com.github.zafarkhaja.semver.Version;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class StandardGson {

    private static final Gson INSTANCE;
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

    public static Gson instance() {
        return INSTANCE;
    }

    public static Gson pretty() {
        return PRETTY_INSTANCE;
    }

    private StandardGson() {
    }
}
