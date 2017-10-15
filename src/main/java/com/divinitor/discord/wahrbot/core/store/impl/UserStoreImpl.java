package com.divinitor.discord.wahrbot.core.store.impl;

import com.divinitor.discord.wahrbot.core.store.UserStore;
import com.divinitor.discord.wahrbot.core.util.gson.StandardGson;
import com.divinitor.discord.wahrbot.core.util.inject.JedisProvider;
import com.divinitor.discord.wahrbot.core.util.redis.RedisList;
import com.divinitor.discord.wahrbot.core.util.redis.RedisMap;
import com.google.inject.Inject;
import net.dv8tion.jda.core.entities.User;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.BiFunction;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class UserStoreImpl implements UserStore {

    private static final String BASE_KEY = "com.divinitor.discord.wahrbot.core.store.user";
    @Inject
    private JedisProvider provider;
    private final User user;


    public UserStoreImpl(User user) {
        this.user = user;
    }

    @Override
    public User getUser() {
        return this.user;
    }

    @Override
    public void purge() {
        try (Jedis j = this.provider.get()) {
            List<String> keys = new ArrayList<>();
            ScanParams params = new ScanParams().match(BASE_KEY + ".*");

            String cursor = "0";
            do {
                ScanResult<String> res = j.scan(cursor, params);
                keys.addAll(res.getResult());
                cursor = res.getStringCursor();
            } while (!cursor.equals("0"));

            j.del(keys.toArray(new String[keys.size()]));
        }
    }

    @Override
    public void put(String key, String value) {
        try (Jedis j = this.provider.get()) {
            j.set(key, value);
        }
    }

    @Override
    public void put(String key, Object value) {
        if (value instanceof Map) {
            //  This will also store it in Redis so we're good
            new RedisMap(this.provider, key(key), StandardGson.instance(), value.getClass(), (Map) value);
        } else if (value instanceof List) {
            //  This will also store it in Redis so we're good
            new RedisList(this.provider, key(key), StandardGson.instance(), value.getClass(), (List) value);
        } else {
            this.put(key(key), this.serializer().apply(value));
        }
    }

    @Override
    public <T> T getObject(String key, Class<T> clazz) {
        if (clazz == String.class) {
            return (T) this.getString(key);
        }

        if (clazz.isAssignableFrom(Map.class)) {
            return (T) new RedisMap<>(this.provider, key(key), StandardGson.instance(), clazz);
        } else if (clazz.isAssignableFrom(List.class)) {
            return (T) new RedisList<>(this.provider, key(key), StandardGson.instance(), clazz);
        }

        return this.deserializer(clazz).apply(this.getString(key), clazz);
    }

    @Override
    public String getString(String key) {
        try (Jedis j = this.provider.get()) {
            return j.get(key);
        }
    }

    @Override
    public <T> BiFunction<String, Class<T>, T> deserializer(Class<T> clazz) {
        return StandardGson.instance()::fromJson;
    }

    @Override
    public <T> Function<T, String> serializer() {
        return StandardGson.instance()::toJson;
    }

    private static String key(String... args) {
        StringJoiner joiner = new StringJoiner(".");
        joiner.add(BASE_KEY);
        for (String arg : args) {
            joiner.add(arg);
        }

        return joiner.toString();
    }
}
