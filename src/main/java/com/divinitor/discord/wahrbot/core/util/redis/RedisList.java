package com.divinitor.discord.wahrbot.core.util.redis;

import com.divinitor.discord.wahrbot.core.util.inject.JedisProvider;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;

import java.lang.reflect.Array;
import java.util.*;

/**
 * An implementation of {@link List} that's backed by Redis.
 * @param <E> The element type
 */
public class RedisList<E> implements List<E> {

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
    private final Class<E> eClass;

    /**
     * Whether or not the generic type is a string. If the generic type is a string, then we can skip serialization and
     * deserialization.
     */
    private final boolean stringType;

    /**
     * Create a RedisList with the given pool, key base, Gson instance, and type.
     * @param pool The Redis connection pool to use
     * @param base The base key to use
     * @param gson The serializer/deserializer to use. Please register any custom type converters as needed.
     * @param eClass The generic type class
     */
    public RedisList(JedisProvider pool, String base, Gson gson, Class<E> eClass) {
        this.pool = pool;
        this.base = base;
        this.gson = gson;
        this.eClass = eClass;
        this.stringType = this.eClass == String.class;
    }

    /**
     * Create a RedisList with the given pool, key base, Gson instance, and type, initialized with the given elements
     * from the provided collection
     * @param pool The Redis connection pool to use
     * @param base The base key to use
     * @param gson The serializer/deserializer to use. Please register any custom type converters as needed.
     * @param eClass The generic type class
     * @param col A collection of elements to initialize this list with
     */
    public RedisList(JedisProvider pool, String base, Gson gson, Class<E> eClass, Collection<E> col) {
        //  Init
        this(pool, base, gson, eClass);
        //  Add elements
        this.addAll(col);
    }

    @Override
    public int size() {
        try (Jedis j = this.pool.getResource()) {
            return Math.toIntExact(j.llen(this.base));
        } catch (ArithmeticException e) {
            //  toIntExact throws an ArithmeticException if we've overflowed an int
            //  the Collection.size() contract states we should return MAX_VALUE if there's more than MAX_VALUE elements
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public boolean isEmpty() {
        return this.size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        try (Jedis j = this.pool.getResource()) {
            //  Pull down the entire list from Redis
            List<String> vals = j.lrange(this.base, 0, -1);
            if (this.stringType) {
                return vals.contains(o);
            } else {
                return vals.stream()
                    //  Deserialize
                    .map(s -> this.gson.fromJson(s, this.eClass))
                    //  Check if any are equal
                    .anyMatch(o::equals);
            }
        }
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        //  Return a simple iterator
        return new Iterator<E>() {
            int index = 0;

            @Override
            public boolean hasNext() {
                //  We have more elements if the pointer is less than the size of the list
                return index < RedisList.this.size();
            }

            @Override
            public E next() {
                //  No more elements
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                //  Get the next element
                E ret = RedisList.this.get(index);
                //  Increment
                ++index;
                return ret;
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
                    //  Deserialize
                    .map(s -> this.gson.fromJson(s, this.eClass))
                    .toArray();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        //  Make sure our types are compatible (required due to type erasure)
        if (!a.getClass().getComponentType().isAssignableFrom(this.eClass)) {
            throw new ArrayStoreException();
        }

        Object[] arr = this.toArray();
        if (a.length < arr.length) {
            //  Make a copy if the provided array is too small
            return Arrays.copyOf(arr, arr.length,
                (Class<? extends T[]>) Array.newInstance(this.eClass, 0).getClass());
        }

        //  Copy the elements over
        System.arraycopy(arr, 0, a, 0, arr.length);
        return a;
    }

    @Override
    public boolean add(E e) {
        //  Cannot accept null objects
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
        //  Cannot accept null objects
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
        //  We don't support inserting elements at a specific index
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
        //  We don't support cherrypicking elements
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
