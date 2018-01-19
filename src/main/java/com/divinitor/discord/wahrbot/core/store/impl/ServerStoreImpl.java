package com.divinitor.discord.wahrbot.core.store.impl;

import com.divinitor.discord.wahrbot.core.store.MemberStore;
import com.divinitor.discord.wahrbot.core.store.ServerStore;
import com.divinitor.discord.wahrbot.core.util.concurrent.Lockable;
import com.divinitor.discord.wahrbot.core.util.discord.SnowflakeUtils;
import com.divinitor.discord.wahrbot.core.util.gson.StandardGson;
import com.divinitor.discord.wahrbot.core.util.inject.JedisProvider;
import com.divinitor.discord.wahrbot.core.util.redis.RedisList;
import com.divinitor.discord.wahrbot.core.util.redis.RedisMap;
import com.divinitor.discord.wahrbot.core.util.redis.RedisSet;
import com.google.inject.Inject;
import com.google.inject.Injector;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ServerStoreImpl implements ServerStore {

    private static final String BASE_KEY = "core.store.server";
    private final Guild guild;
    private final Injector injector;
    private final ReadWriteLock lock;
    private final TLongObjectMap<WeakReference<MemberStoreImpl>> cache;
    @Inject
    private JedisProvider provider;
    private RedisMap<String> looseParams;

    public ServerStoreImpl(Guild guild, Injector injector) {
        this.guild = guild;
        this.injector = injector;
        this.lock = new ReentrantReadWriteLock();
        this.cache = new TLongObjectHashMap<>();
    }

    private String key(String... args) {
        StringJoiner joiner = new StringJoiner(".");
        joiner.add(BASE_KEY);
        joiner.add(SnowflakeUtils.encode(this.guild));
        for (String arg : args) {
            joiner.add(arg);
        }

        return joiner.toString();
    }

    @Override
    public Guild getServer() {
        return this.guild;
    }

    @Override
    public MemberStore forMember(Member member) {
        WeakReference<MemberStoreImpl> ret;
        MemberStoreImpl rett;
        try (Lockable rl = Lockable.acquire(this.lock.readLock())) {
            ret = this.cache.get(member.getUser().getIdLong());
        }

        if (ret == null || ret.get() == null) {
            try (Lockable wl = Lockable.acquire(this.lock.writeLock())) {
                //  Re-check
                ret = this.cache.get(member.getUser().getIdLong());
                if (ret == null || ret.get() == null) {
                    rett = new MemberStoreImpl(member);
                    this.injector.injectMembers(rett);
                    ret = new WeakReference<>(rett);
                    this.cache.put(member.getUser().getIdLong(), ret);
                } else {
                    rett = ret.get();
                }
            }
        } else {
            rett = ret.get();
        }

        return rett;
    }

    @Override
    public void purge() {
        try (Jedis j = this.provider.get()) {
            List<String> keys = new ArrayList<>();
            ScanParams params = new ScanParams().match(this.key("*"));

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
        if (this.looseParams == null) {
            this.looseParams = new RedisMap<>(provider, this.key(), StandardGson.instance(), String.class);
        }

        this.looseParams.put(key, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void put(String key, Object value) {
        if (value instanceof String) {
            this.put(key, (String) value);
        } else if (value instanceof Map) {
            //  This will also store it in Redis so we're good
            new RedisMap(this.provider, key(key), StandardGson.instance(), value.getClass(), (Map) value);
        } else if (value instanceof List) {
            //  This will also store it in Redis so we're good
            new RedisList(this.provider, key(key), StandardGson.instance(), value.getClass(), (List) value);
        } else if (value instanceof Set) {
            //  This will also store it in Redis so we're good
            new RedisSet(this.provider, key(key), StandardGson.instance(), value.getClass(), (Set) value);
        } else {
            this.put(this.key(key), this.serializer().apply(value));
        }
    }

    @Override
    public <T> T getObject(String key, Class<T> clazz) {
        return this.getObject(key, clazz, String.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, V> T getObject(String key, Class<T> clazz, Class<V> vClass) {
        if (clazz == String.class) {
            return (T) this.getString(key);
        } else if (clazz.isAssignableFrom(Map.class)) {
            return (T) new RedisMap<>(this.provider, this.key(key), StandardGson.instance(), vClass);
        } else if (clazz.isAssignableFrom(List.class)) {
            return (T) new RedisList<>(this.provider, this.key(key), StandardGson.instance(), vClass);
        } else if (clazz.isAssignableFrom(Set.class)) {
            return (T) new RedisSet<>(this.provider, this.key(key), StandardGson.instance(), vClass);
        } else {
            return this.deserializer(clazz).apply(this.getString(key), clazz);
        }
    }

    @Override
    public String getString(String key) {
        if (this.looseParams == null) {
            this.looseParams = new RedisMap<>(provider, this.key(), StandardGson.instance(), String.class);
        }

        return this.looseParams.get(key);
    }

    @Override
    public <T> BiFunction<String, Class<T>, T> deserializer(Class<T> clazz) {
        return StandardGson.instance()::fromJson;
    }

    @Override
    public <T> Function<T, String> serializer() {
        return StandardGson.instance()::toJson;
    }
}
