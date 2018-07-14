package com.divinitor.discord.wahrbot.core.util.redis;

import com.divinitor.discord.wahrbot.core.util.inject.JedisProvider;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanResult;

import java.util.*;

/**
 * An implementation of {@link Set} that's backed by Redis.
 * @param <V> The value type
 */
public class RedisSet<V> implements Set<V> {

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
     * Create a RedisSet with the given pool, key base, Gson instance, and type.
     * @param pool The Redis connection pool to use
     * @param base The base key to use
     * @param gson The serializer/deserializer to use. Please register any custom type converters as needed.
     * @param vClass The generic type class
     */
    public RedisSet(JedisProvider pool, String base, Gson gson, Class<V> vClass) {
        this.pool = pool;
        this.base = base;
        this.gson = gson;
        this.vClass = vClass;
        this.stringType = this.vClass == String.class;
    }

    /**
     * Create a RedisSet with the given pool, key base, Gson instance, and type, initialized with the given elements
     * from the provided collection
     * @param pool The Redis connection pool to use
     * @param base The base key to use
     * @param gson The serializer/deserializer to use. Please register any custom type converters as needed.
     * @param vClass The generic type class
     * @param c A collection of elements to initialize this set with
     */
    public RedisSet(JedisProvider pool, String base, Gson gson, Class<V> vClass, Collection<V> c) {
        this(pool, base, gson, vClass);
        this.addAll(c);
    }

    @Override
    public int size() {
        try (Jedis j = this.pool.getResource()) {
            return Math.toIntExact(j.scard(this.base));
        }
    }

    @Override
    public boolean isEmpty() {
        return this.size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        if (!this.vClass.isInstance(o)) {
            return false;
        }

        try (Jedis j = this.pool.getResource()) {
            String s;
            if (this.stringType) {
                s = (String) o;
            } else {
                s = this.gson.toJson(o);
            }

            return j.sismember(this.base, s);
        }
    }

    @NotNull
    @Override
    public Iterator<V> iterator() {
        return new Iterator<V>() {

            String cursor = "";
            Deque<String> buffer = new LinkedList<>();

            @Override
            public boolean hasNext() {
                return !this.buffer.isEmpty() || this.buffer() != 0;
            }

            @SuppressWarnings("unchecked")
            @Override
            public V next() {
                if (this.hasNext()) {
                    String val = this.buffer.pop();
                    if (RedisSet.this.stringType) {
                        return (V) val;
                    } else {
                        return RedisSet.this.gson.fromJson(val, RedisSet.this.vClass);
                    }
                } else {
                    throw new NoSuchElementException();
                }
            }

            private int buffer() {
                try (Jedis j = RedisSet.this.pool.getResource()) {
                    if (this.cursor.equalsIgnoreCase("0")) {
                        return 0;
                    } else if (this.cursor.isEmpty()) {
                        this.cursor = "0";
                    }

                    ScanResult<String> ret = j.sscan(RedisSet.this.base, cursor);
                    this.cursor = ret.getStringCursor();
                    this.buffer.addAll(ret.getResult());
                    return ret.getResult().size();
                }
            }
        };
    }

    @NotNull
    @Override
    public Object[] toArray() {
        List<V> vals = new ArrayList<>();
        vals.addAll(this);
        return vals.toArray();
    }

    @NotNull
    @Override
    public <T1> T1[] toArray(@NotNull T1[] a) {
        List<V> vals = new ArrayList<>();
        vals.addAll(this);
        return vals.toArray(a);
    }

    @Override
    public boolean add(V v) {
        try (Jedis j = this.pool.getResource()) {
            String val;
            if (this.stringType) {
                val = (String) v;
            } else {
                val = this.gson.toJson(v);
            }

            return j.sadd(this.base, val) > 0;
        }
    }

    @Override
    public boolean remove(Object o) {
        if (!this.vClass.isInstance(o)) {
            return false;
        }

        try (Jedis j = this.pool.getResource()) {
            String val;
            if (this.stringType) {
                val = (String) o;
            } else {
                val = this.gson.toJson(o);
            }

            return j.srem(this.base, val) > 0;
        }
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        boolean has = true;

        for (Object o : c) {
            has &= this.contains(o);
        }

        return has;
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends V> c) {
        boolean added = false;

        for (V v : c) {
            added |= this.add(v);
        }

        return added;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        boolean removed = false;

        for (Object o : c) {
            removed |= this.remove(o);
        }

        return removed;
    }

    @Override
    public void clear() {
        try (Jedis j = this.pool.getResource()) {
            j.del(this.base);
        }
    }
}
