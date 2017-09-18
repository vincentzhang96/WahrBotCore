package com.divinitor.discord.wahrbot.core.config.dyn.impl;

import com.divinitor.discord.wahrbot.core.config.dyn.*;
import com.divinitor.discord.wahrbot.core.util.inject.JedisProvider;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import redis.clients.jedis.Jedis;

import java.util.function.BiFunction;
import java.util.function.Function;

public class RedisDynConfigStore implements DynConfigStore {

    @Inject
    private JedisProvider provider;

    private final Gson gson;

    public RedisDynConfigStore() {
        this.gson = new GsonBuilder().create();
    }

    @Override
    public void put(String key, String value) {
        try (Jedis j = this.provider.get()) {
            j.set(key, value);
        }
    }

    @Override
    public String getString(String key) {
        try (Jedis j = this.provider.get()) {
            return j.get(key);
        }
    }

    @Override
    public <T> BiFunction<String, Class<T>, T> deserializer(Class<T> clazz) {
        return this.gson::fromJson;
    }

    @Override
    public <T> Function<T, String> serializer() {
        return this.gson::toJson;
    }
}
