package com.divinitor.discord.wahrbot.core.util.redis;

import com.divinitor.discord.wahrbot.core.util.inject.JedisProvider;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

public class RedisList<E> implements List<E> {

    private final JedisProvider pool;
    private final String base;
    private final Gson gson;
    private final Class<E> eClass;
    private final boolean stringType;

    public RedisList(JedisProvider pool, String base, Gson gson, Class<E> eClass) {
        this.pool = pool;
        this.base = base;
        this.gson = gson;
        this.eClass = eClass;
        this.stringType = this.eClass == String.class;
    }


    public RedisList(JedisProvider pool, String base, Gson gson, Class<E> eClass, Collection<E> col) {
        this(pool, base, gson, eClass);
        this.addAll(col);
    }

    @Override
    public int size() {
        try (Jedis j = this.pool.getResource()) {
            return Math.toIntExact(j.llen(this.base));
        }
    }

    @Override
    public boolean isEmpty() {
        return this.size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        try (Jedis j = this.pool.getResource()) {
            List<String> vals = j.lrange(this.base, 0, -1);
            if (this.stringType) {
                return vals.contains(o);
            } else {
                return vals.stream()
                    .map(s -> this.gson.fromJson(s, this.eClass))
                    .anyMatch(o::equals);
            }
        }
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            int index = 0;

            @Override
            public boolean hasNext() {
                return index + 1 < RedisList.this.size();
            }

            @Override
            public E next() {
                return RedisList.this.get(index);
            }
        };
    }

    @NotNull
    @Override
    public Object[] toArray() {
        try (Jedis j = this.pool.getResource()) {
            List<String> vals = j.lrange(this.base, 0, -1);
            if (this.stringType) {
                return vals.toArray();
            } else {
                return vals.stream()
                    .map(s -> this.gson.fromJson(s, this.eClass))
                    .collect(Collectors.toList())
                    .toArray();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        if (!a.getClass().isAssignableFrom(this.eClass)) {
            throw new ArrayStoreException();
        }

        Object[] arr = this.toArray();
        if (a.length < arr.length) {
            return Arrays.copyOf(arr, arr.length,
                (Class<? extends T[]>) Array.newInstance(this.eClass, 0).getClass());
        }

        System.arraycopy(arr, 0, a, 0, arr.length);
        return a;
    }

    @Override
    public boolean add(E e) {
        if (e == null) {
            throw new NullPointerException();
        }

        try (Jedis j = this.pool.getResource()) {
            if (this.stringType) {
                j.rpush(this.base, (String) e);
            } else {
                j.rpush(this.base, this.gson.toJson(e));
            }
        }

        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (o == null) {
            throw new NullPointerException();
        }

        try (Jedis j = this.pool.getResource()) {
            if (this.stringType) {
                return j.lrem(this.base, 1, (String) o) > 0;
            } else {
                return j.lrem(this.base, 1, this.gson.toJson(o)) > 0;
            }
        }
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        for (Object o : c) {
            if (!this.contains(o)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        boolean changed = false;

        for (E e : c) {
            changed |= this.add(e);
        }

        return changed;
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        boolean changed = false;

        for (Object o : c) {
            changed |= this.remove(o);
        }

        return changed;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        try (Jedis j = this.pool.getResource()) {
            j.del(this.base);
        }
    }

    @Override
    public E get(int index) {
        try (Jedis j = this.pool.getResource()) {
            return getImpl(index, j);
        }
    }

    @SuppressWarnings("unchecked")
    private E getImpl(int index, Jedis j) {
        String val = j.lindex(this.base, index);
        if (this.stringType) {
            return (E) val;
        } else {
            return this.gson.fromJson(val, this.eClass);
        }
    }

    @Override
    public E set(int index, E element) {
        try (Jedis j = this.pool.getResource()) {
            E ret = getImpl(index, j);
            if (this.stringType) {
                j.lset(this.base, index, (String) element);
            } else {
                j.lset(this.base, index, this.gson.toJson(element));
            }

            return ret;
        }
    }

    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(Object o) {
        if (!this.eClass.isInstance(o)) {
            return -1;
        }

        if (this.stringType) {
            try (Jedis j = this.pool.getResource()) {
                List<String> vals = j.lrange(this.base, 0, -1);
                return vals.indexOf(o);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public int lastIndexOf(Object o) {
        if (!this.eClass.isInstance(o)) {
            return -1;
        }

        if (this.stringType) {
            try (Jedis j = this.pool.getResource()) {
                List<String> vals = j.lrange(this.base, 0, -1);
                return vals.lastIndexOf(o);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator(int index) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }
}
