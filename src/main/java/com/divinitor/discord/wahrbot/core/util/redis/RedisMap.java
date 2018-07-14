package com.divinitor.discord.wahrbot.core.util.redis;

import com.divinitor.discord.wahrbot.core.util.inject.JedisProvider;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An implementation of {@link Map} that's backed by Redis. Keys must be strings.
 * @param <V> The value type
 */
public class RedisMap<V> implements Map<String, V> {

    /**
     * The Redis connection pool
     */
    private final JedisProvider pool;

    /**
     * The base key/key prefix to use.
     */
    private final String base;

    /**
     * JSON serializer/deserializer
     */
    private final Gson gson;

    /**
     * The class of the generic type (so we can properly serialize/deserialize), as generic type information is erased
     * at runtime
     */
    private final Class<V> vClass;

    /**
     * Whether or not the generic type is a string. If the generic type is a string, then we can skip serialization and
     * deserialization.
     */
    private final boolean stringType;

    /**
     * Create a RedisMap with the given pool, key base, Gson instance, and type.
     * @param pool The Redis connection pool to use
     * @param base The base key to use
     * @param gson The serializer/deserializer to use. Please register any custom type converters as needed.
     * @param vClass The generic type class
     */
    public RedisMap(JedisProvider pool, String base, Gson gson, Class<V> vClass) {
        this.pool = pool;
        this.base = base;
        this.gson = gson;
        this.vClass = vClass;
        this.stringType = this.vClass == String.class;
    }

    /**
     * Create a RedisMap with the given pool, key base, Gson instance, and type, initialized with the given elements
     * from the provided collection
     * @param pool The Redis connection pool to use
     * @param base The base key to use
     * @param gson The serializer/deserializer to use. Please register any custom type converters as needed.
     * @param vClass The generic type class
     * @param val A map of elements to initialize this map with
     */
    public RedisMap(JedisProvider pool, String base, Gson gson, Class<V> vClass, Map<String, V> val) {
        this(pool, base, gson, vClass);
        this.putAll(val);
    }

    @Override
    public int size() {
        try (Jedis j = this.pool.getResource()) {
            return Math.toIntExact(j.hlen(this.base));
        }
    }

    @Override
    public boolean isEmpty() {
        return this.size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        if (key instanceof String) {
            try (Jedis j = this.pool.getResource()) {
                return j.hexists(this.base, (String) key);
            }
        }

        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        try (Jedis j = this.pool.getResource()) {
            return j.hgetAll(this.base).containsValue(value);
        }
    }

    @Override
    public V get(Object key) {
        if (!(key instanceof String)) {
            return null;
        }

        String sKey = (String) key;

        try (Jedis j = this.pool.getResource()) {
            return getImpl(sKey, j);
        }
    }

    @SuppressWarnings("unchecked")
    private V getImpl(String sKey, Jedis j) {
        String ret = j.hget(this.base, sKey);
        if (ret == null) {
            return null;
        }

        if (stringType) {
            return (V) ret;
        } else {
            return this.gson.fromJson(ret, vClass);
        }
    }

    @Override
    public V put(String key, V value) {
        try (Jedis j = this.pool.getResource()) {
            V old = this.getImpl(key, j);
            putImpl(key, value, j);

            return old;
        }
    }

    private void putImpl(String key, V value, Jedis j) {
        if (stringType) {
            j.hset(this.base, key, (String) value);
        } else {
            j.hset(this.base, key, this.gson.toJson(value));
        }
    }

    @Override
    public V remove(Object key) {
        if (!(key instanceof String)) {
            return null;
        }

        String sKey = (String) key;
        try (Jedis j = this.pool.getResource()) {
            V old = this.getImpl(sKey, j);
            j.hdel(this.base, sKey);
            return old;
        }
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ? extends V> m) {
        try (Jedis j = this.pool.getResource()) {
            m.forEach((k, v) -> {
                this.putImpl(k, v, j);
            });
        }
    }

    @Override
    public void clear() {
        try (Jedis j = this.pool.getResource()) {
            j.del(this.base);
        }
    }

    @NotNull
    @Override
    public Set<String> keySet() {
        try (Jedis j = this.pool.getResource()) {
            return j.hgetAll(this.base).keySet();
        }
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public Collection<V> values() {
        try (Jedis j = this.pool.getResource()) {
            Collection<String> values = j.hgetAll(this.base).values();
            if (stringType) {
                return (Collection<V>) values;
            } else {
                return values.stream()
                    .map(s -> gson.fromJson(s, this.vClass))
                    .collect(Collectors.toList());
            }
        }
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public Set<Entry<String, V>> entrySet() {
        try (Jedis j = this.pool.getResource()) {
            return j.hkeys(this.base).stream()
                .map(RMEntry::new)
                .collect(Collectors.toSet());
        }
    }

    private class RMEntry implements Map.Entry<String, V> {

        private final String key;

        RMEntry(String key) {
            this.key = key;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return RedisMap.this.get(this.getKey());
        }

        @Override
        public V setValue(V value) {
            return RedisMap.this.put(this.getKey(), value);
        }
    }
}
