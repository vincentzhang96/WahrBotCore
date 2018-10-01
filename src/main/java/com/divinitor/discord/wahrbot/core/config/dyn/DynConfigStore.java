package com.divinitor.discord.wahrbot.core.config.dyn;

import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface DynConfigStore {

    void put(String key, String value);

    default void put(String key, long value) {
        this.put(key, Long.toString(value));
    }

    default void put(String key, double value) {
        this.put(key, Double.toString(value));
    }

    default void put(String key, boolean value) {
        this.put(key, Boolean.toString(value));
    }

    default void put(String key, Object value) {
        this.put(key, this.serializer().apply(value));
    }

    String getString(String key);

    default String getString(String key, String def) {
        String val = this.getString(key);
        if (val == null) {
            return def;
        }

        return val;
    }

    default long getLong(String key) {
        String val = this.getString(key);
        if (val == null) {
            throw new NoSuchElementException(key);
        }

        try {
            return Long.parseLong(val);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(key + " is not an integer");
        }
    }

    default long getLong(String key, long def) {
        try {
            return this.getLong(key);
        } catch (Exception e) {
            return def;
        }
    }

    default int getInt(String key) {
        return (int) getLong(key);
    }

    default int getInt(String key, int def) {
        try {
            return this.getInt(key);
        } catch (Exception e) {
            return def;
        }
    }

    default double getDouble(String key) {
        String val = this.getString(key);
        if (val == null) {
            throw new NoSuchElementException(key);
        }

        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(key + " is not a real");
        }
    }

    default double getDouble(String key, double def) {
        try {
            return this.getDouble(key);
        } catch (Exception e) {
            return def;
        }
    }

    default float getFloat(String key) {
        return (float) getDouble(key);
    }

    default float getFloat(String key, float def) {
        try {
            return this.getFloat(key);
        } catch (Exception e) {
            return def;
        }
    }

    default boolean getBoolean(String key) {
        String val = this.getString(key);
        if (val == null) {
            throw new NoSuchElementException(key);
        }

        try {
            return Boolean.parseBoolean(val);
        } catch (Exception e) {
            throw new IllegalArgumentException(key + " is not a boolean");
        }
    }

    default boolean getBoolean(String key, boolean def) {
        try {
            return this.getBoolean(key);
        } catch (Exception e) {
            return def;
        }
    }

    default <T> T getObject(String key, Class<T> clazz) {
        String val = this.getString(key);
        if (val == null) {
            return null;
        }

        return this.deserializer(clazz).apply(val, clazz);
    }

    default <T> T getObject(String key, Class<T> clazz, T def) {
        try {
            T ret = this.getObject(key, clazz);
            if (ret == null) {
                return def;
            }

            return ret;
        } catch (Exception e) {
            return def;
        }
    }

    default <T, V> T getObject(String key, Class<T> clazz, Class<V> vClass) {
        return this.getObject(key, clazz);
    }

    default <T, V> T getObject(String key, Class<T> clazz, Class<V> vClass, T def) {
        try {
            T ret = this.getObject(key, clazz, vClass);
            if (ret == null) {
                return def;
            }

            return ret;
        } catch (Exception e) {
            return def;
        }
    }

    default DynConfigHandle getStringHandle(String key) {
        return () -> this.getString(key);
    }

    default LongDynConfigHandle getLongHandle(String key) {
        return () -> this.getLong(key);
    }

    default DoubleDynConfigHandle getDoubleHandle(String key) {
        return () -> this.getDouble(key);
    }

    default BooleanDynConfigHandle getBooleanHandle(String key) {
        return () -> this.getBoolean(key);
    }

    default <T> ObjectDynConfigHandle<T> getObjectHandle(String key, Class<T> clazz) {
        return () -> this.getObject(key, clazz);
    }

    <T> BiFunction<String, Class<T>, T> deserializer(Class<T> clazz);

    <T> Function<T, String> serializer();

}
