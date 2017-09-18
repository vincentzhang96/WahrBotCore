package com.divinitor.discord.wahrbot.core.config.dyn;

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

    default long getLong(String key) {
        return Long.parseLong(this.getString(key));
    }

    default int getInt(String key) {
        return (int) getLong(key);
    }

    default double getDouble(String key) {
        return Double.parseDouble(this.getString(key));
    }

    default float getFloat(String key) {
        return (float) getDouble(key);
    }

    default boolean getBoolean(String key) {
        return Boolean.parseBoolean(this.getString(key));
    }

    default <T> T getObject(String key, Class<T> clazz) {
        return this.deserializer(clazz).apply(this.getString(key), clazz);
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
