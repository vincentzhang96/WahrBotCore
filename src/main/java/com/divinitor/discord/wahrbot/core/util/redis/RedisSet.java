package com.divinitor.discord.wahrbot.core.util.redis;

import com.divinitor.discord.wahrbot.core.util.inject.JedisProvider;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanResult;

import java.util.*;

public class RedisSet<V> implements Set<V> {

    private final JedisProvider pool;
    private final String base;
    private final Gson gson;
    private final Class<V> vClass;
    private final boolean stringType;

    public RedisSet(JedisProvider pool, String base, Gson gson, Class<V> vClass) {
        this.pool = pool;
        this.base = base;
        this.gson = gson;
        this.vClass = vClass;
        this.stringType = this.vClass == String.class;
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

            String cursor = "0";
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
        return new Object[0];
    }

    @NotNull
    @Override
    public <T1> T1[] toArray(@NotNull T1[] a) {
        return null;
    }

    @Override
    public boolean add(V v) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends V> c) {
        return false;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return false;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {

    }
}
